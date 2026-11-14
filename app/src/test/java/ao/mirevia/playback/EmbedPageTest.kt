package ao.mirevia.playback

import ao.mirevia.core.playback.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class EmbedPageTest {
    private val policy = EmbedUrlPolicy(setOf("myembed.biz"))

    @Test
    fun `wrapper contains a real iframe and official permissions`() = runBlocking {
        val source = EmbedMoviesProvider(policy.allowedHosts).getMovieSource(11, null, null, null)!!
        val html = EmbedPage.html(source, policy)
        assertTrue(html.contains("<iframe src=\"https://myembed.biz/filme/11\""))
        assertTrue(html.contains("allowfullscreen"))
        assertTrue(html.contains("encrypted-media"))
        assertTrue(html.contains("frame-src https://myembed.biz"))
    }

    @Test
    fun `wrapper has no script bridge or media element`() = runBlocking {
        val source = EmbedMoviesProvider(policy.allowedHosts).getSeriesSource(30984, 1, 1)!!
        val html = EmbedPage.html(source, policy)
        assertFalse(html.contains("<script"))
        assertFalse(html.contains("<video"))
        assertFalse(html.contains("<audio"))
        assertFalse(html.contains("postMessage"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cannot embed an arbitrary host`() {
        EmbedPage.html(
            VideoSource(
                "https://evil.example/filme/11",
                VideoSourceType.IFRAME,
                "Unknown",
                true,
                true,
                true,
                false,
            ),
            policy,
        )
    }
}
