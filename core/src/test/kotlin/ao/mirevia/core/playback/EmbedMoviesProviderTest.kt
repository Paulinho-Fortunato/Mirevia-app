package ao.mirevia.core.playback

import ao.mirevia.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EmbedMoviesProviderTest {
    private val provider = EmbedMoviesProvider(setOf("myembed.biz"))

    @Test
    fun `movie uses documented TMDB route`() = runBlocking {
        val source = provider.getMovieSource(11, null, null, null)!!
        assertEquals("https://myembed.biz/filme/11", source.url)
        assertEquals(VideoSourceType.IFRAME, source.type)
        assertTrue(source.isIframe && source.requiresWebView && source.supportsFullscreen)
        assertFalse(source.supportsPictureInPicture)
        assertNull(source.subtitlesUrl)
    }

    @Test
    fun `movie uses valid IMDb without searching by title`() = runBlocking {
        assertEquals(
            "https://myembed.biz/filme/tt0076759",
            provider.getMovieSource(11, "tt0076759", "ignored", 1977)?.url,
        )
    }

    @Test
    fun `rejects invalid movie identifiers without fallback`() = runBlocking {
        for (id in listOf(0, -1, Int.MIN_VALUE)) assertNull(
            provider.getMovieSource(id, "tt0076759", null, null)
        )
        for (imdb in
            listOf(
                "",
                " ",
                "tt",
                "tt123",
                "nm0000001",
                "tt0000001/1",
                "tt0076759?x=1",
                "TT0076759",
                " tt0076759",
            )) {
            assertNull(provider.getMovieSource(11, imdb, "Cannot create an alternative", null))
        }
    }

    @Test
    fun `series list and episode use only documented routes`() = runBlocking {
        assertEquals("https://myembed.biz/serie/30984", provider.getSeriesSource(30984)?.url)
        assertEquals(
            "https://myembed.biz/serie/30984/1/2",
            provider.getSeriesSource(30984, 1, 2)?.url,
        )
    }

    @Test
    fun `partial or invalid season episode is rejected`() = runBlocking {
        assertNull(provider.getSeriesSource(0))
        assertNull(provider.getSeriesSource(-1))
        for ((season, episode) in listOf(1 to null, null to 1, 0 to 1, 1 to 0, -1 to 2, 1 to -1)) {
            assertNull(provider.getSeriesSource(30984, season, episode))
        }
    }

    @Test
    fun `unapproved documented host disables the provider`() = runBlocking {
        val restricted = EmbedMoviesProvider(setOf("embedmovies.org"))
        assertNull(restricted.getMovieSource(11, null, null, null))
        assertNull(restricted.getSeriesSource(30984))
    }

    @Test
    fun `demo identifiers can never become a source`() = runBlocking {
        val resolve = ResolveVideoSource(provider, EmbedUrlPolicy(setOf("myembed.biz")))
        val details =
            MediaDetails(MediaSummary(MediaKey(DataOrigin.DEMO, MediaType.MOVIE, 11), "Exemplo"))
        assertNull(resolve(details))
    }

    @Test
    fun `resolution validates the provider output again`() = runBlocking {
        val bad =
            object : VideoSourceProvider {
                override suspend fun getMovieSource(
                    tmdbId: Int,
                    imdbId: String?,
                    title: String?,
                    year: Int?,
                ) =
                    VideoSource(
                        "https://evil.example/filme/11",
                        VideoSourceType.IFRAME,
                        "External",
                        true,
                        true,
                        false,
                        false,
                    )

                override suspend fun getSeriesSource(tmdbId: Int, season: Int?, episode: Int?) =
                    null
            }
        val resolve = ResolveVideoSource(bad, EmbedUrlPolicy(setOf("myembed.biz")))
        assertNull(
            resolve(
                MediaDetails(MediaSummary(MediaKey(DataOrigin.TMDB, MediaType.MOVIE, 11), "TMDB"))
            )
        )
    }
}
