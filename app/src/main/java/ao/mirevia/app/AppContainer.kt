package ao.mirevia.app

import android.content.Context
import androidx.room.Room
import ao.mirevia.BuildConfig
import ao.mirevia.core.playback.*
import ao.mirevia.core.repository.CatalogRepository
import ao.mirevia.data.demo.DemoCatalogRepository
import ao.mirevia.data.local.MireviaDatabase
import ao.mirevia.data.local.PreferencesRepository
import ao.mirevia.data.network.NetworkMonitor
import ao.mirevia.data.remote.tmdb.TmdbClient
import ao.mirevia.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    // Lets an already-started local history transaction finish when the player Activity closes.
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database =
        Room.databaseBuilder(context, MireviaDatabase::class.java, "mirevia.db").build()
    val library = RoomLibraryRepository(database)
    val preferences = PreferencesRepository(context)
    val network = NetworkMonitor(context)
    val catalog: CatalogRepository =
        when {
            BuildConfig.TMDB_PROXY_BASE_URL.isBlank() && BuildConfig.TMDB_READ_TOKEN.isBlank() ->
                DemoCatalogRepository()
            else ->
                try {
                    TmdbCatalogRepository(
                        TmdbClient.create(
                            context.cacheDir,
                            BuildConfig.TMDB_PROXY_BASE_URL,
                            BuildConfig.TMDB_READ_TOKEN,
                        )
                    )
                } catch (_: ao.mirevia.core.model.ConfigurationException) {
                    UnconfiguredCatalogRepository()
                }
        }
    // Initial playback/share sources only. Keep these exact-host routes separate from the
    // owner's consented HTTPS resource/frame policy in IframeRequestPolicy.
    val embedPolicy = EmbedUrlPolicy(setOf(EmbedMoviesUrlBuilder.DOCUMENTED_HOST))
    private val videoProvider: VideoSourceProvider = EmbedMoviesProvider(embedPolicy.allowedHosts)
    val resolveVideoSource = ResolveVideoSource(videoProvider, embedPolicy)
    val connectionLabel: String
        get() =
            when {
                catalog.isDemo -> "Demonstração · TMDB não configurada"
                catalog is UnconfiguredCatalogRepository -> "Configuração TMDB inválida"
                BuildConfig.TMDB_PROXY_BASE_URL.isNotBlank() -> "TMDB · backend intermediário"
                else -> "TMDB · configuração local de desenvolvimento"
            }
}
