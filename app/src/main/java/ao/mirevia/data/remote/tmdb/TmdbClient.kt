package ao.mirevia.data.remote.tmdb

import ao.mirevia.core.model.ConfigurationException
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object TmdbClient {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    fun create(cacheDir: File, proxyBaseUrl: String, readToken: String): TmdbApi {
        val base =
            (proxyBaseUrl.ifBlank { "https://api.themoviedb.org/3/" }).toHttpUrlOrNull()
                ?: throw ConfigurationException()
        if (
            !base.isHttps ||
                base.username.isNotEmpty() ||
                base.password.isNotEmpty() ||
                base.query != null ||
                base.fragment != null ||
                !base.encodedPath.endsWith("/") ||
                base.port != 443
        )
            throw ConfigurationException()
        if (proxyBaseUrl.isBlank() && readToken.isBlank()) throw ConfigurationException()
        val client =
            OkHttpClient.Builder()
                .cache(Cache(File(cacheDir, "tmdb"), 24L * 1024 * 1024))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
                .addInterceptor(
                    Interceptor { chain ->
                        val original = chain.request()
                        if (
                            original.url.scheme != "https" ||
                                original.url.host != base.host ||
                                original.url.port != base.port
                        ) {
                            throw java.io.IOException("Unexpected API origin")
                        }
                        val request =
                            original
                                .newBuilder()
                                .header("Accept", "application/json")
                                .header("Cache-Control", "max-age=300")
                                .apply {
                                    // A proxy supplies its own upstream credentials. Never send the
                                    // local token to a proxy.
                                    if (proxyBaseUrl.isBlank())
                                        header("Authorization", "Bearer $readToken")
                                }
                                .build()
                        chain.proceed(request)
                    }
                )
                .build()
        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TmdbApi::class.java)
    }
}

/** Conservative client-side policy, not a claim about TMDB's current server limit. */
class ApiRequestGate {
    private val permits = Semaphore(4)
    private val pacing = Mutex()
    private var lastStartNanos = 0L

    suspend fun <T> execute(block: suspend () -> T): T =
        permits.withPermit {
            suspend fun paced(): T {
                pacing.withLock {
                    val remaining = 250L - (System.nanoTime() - lastStartNanos) / 1_000_000
                    if (remaining > 0) delay(remaining)
                    lastStartNanos = System.nanoTime()
                }
                return block()
            }
            try {
                paced()
            } catch (error: HttpException) {
                if (error.code() != 429) throw error
                val retryAfter = error.response()?.headers()?.get("Retry-After")?.toLongOrNull()
                // Do not retry early when the server requests a longer wait.
                if (retryAfter != null && retryAfter > 30) throw error
                delay((retryAfter ?: 2L).coerceIn(1, 30) * 1_000L)
                paced() // Exactly one retry; never retry 401, 403 or arbitrary provider requests.
            }
        }
}
