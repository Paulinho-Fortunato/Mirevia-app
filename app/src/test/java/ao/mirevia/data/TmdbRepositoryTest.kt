package ao.mirevia.data

import ao.mirevia.core.model.*
import ao.mirevia.data.remote.tmdb.*
import ao.mirevia.data.repository.TmdbCatalogRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class TmdbRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: TmdbCatalogRepository

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        // HTTP is used ONLY for this loopback test fixture, never by TmdbClient in the application.
        val api =
            Retrofit.Builder()
                .baseUrl(server.url("/3/"))
                .client(OkHttpClient())
                .addConverterFactory(
                    Json { ignoreUnknownKeys = true }
                        .asConverterFactory("application/json".toMediaType())
                )
                .build()
                .create(TmdbApi::class.java)
        repository = TmdbCatalogRepository(api)
    }

    @After
    fun cleanup() {
        server.shutdown()
    }

    private fun config() {
        server.enqueue(
            MockResponse()
                .setBody(
                    """{"images":{"secure_base_url":"https://image.tmdb.org/t/p/","poster_sizes":["w342"]}}"""
                )
        )
    }

    @Test
    fun `search uses official endpoint and preserves pagination`() = runTest {
        config()
        server.enqueue(
            MockResponse()
                .setBody(
                    """{"page":1,"total_pages":2,"results":[{"id":11,"media_type":"movie","title":"Fixture"}]}"""
                )
        )
        val page = repository.search("Fixture", SearchScope.ALL, 1)
        assertTrue(page.hasMore)
        assertEquals(1, page.items.size)
        assertEquals("/3/configuration", server.takeRequest().requestUrl!!.encodedPath)
        val request = server.takeRequest().requestUrl!!
        assertEquals("/3/search/multi", request.encodedPath)
        assertEquals("Fixture", request.queryParameter("query"))
        assertEquals("false", request.queryParameter("include_adult"))
    }

    @Test
    fun `empty response stays empty`() = runTest {
        config()
        server.enqueue(MockResponse().setBody("""{"page":1,"total_pages":0,"results":[]}"""))
        assertTrue(repository.search("Unknown", SearchScope.MOVIES, 1).items.isEmpty())
    }

    @Test
    fun `unauthorized response is not replaced by demo`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))
        try {
            repository.search("Fixture", SearchScope.ALL, 1)
            fail("Expected 401")
        } catch (error: HttpException) {
            assertEquals(401, error.code())
        }
    }

    @Test
    fun `rate limit with short retry after retries only once`() = runTest {
        config()
        server.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "1"))
        server.enqueue(MockResponse().setBody("""{"page":1,"total_pages":0,"results":[]}"""))
        assertTrue(repository.search("Fixture", SearchScope.ALL, 1).items.isEmpty())
        assertEquals(3, server.requestCount)
    }
}
