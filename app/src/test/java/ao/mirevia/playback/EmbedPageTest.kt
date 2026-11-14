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

    @Test
    fun `documented sample routes remain untouched inside the iframe`() = runBlocking {
        val provider = EmbedMoviesProvider(policy.allowedHosts)
        val sources =
            listOf(
                provider.getMovieSource(969681, null, null, null)!!,
                provider.getMovieSource(969681, "tt22084616", null, null)!!,
                provider.getSeriesSource(30984, null, null)!!,
                provider.getSeriesSource(30984, 1, 1)!!,
            )
        val urls =
            listOf(
                "https://myembed.biz/filme/969681",
                "https://myembed.biz/filme/tt22084616",
                "https://myembed.biz/serie/30984",
                "https://myembed.biz/serie/30984/1/1",
            )
        sources.zip(urls).forEach { (source, expected) ->
            assertEquals(expected, source.url)
            assertTrue(EmbedPage.html(source, policy).contains("<iframe src=\"$expected\""))
        }
    }

    @Test
    fun `iframe is responsive without referrer spoofing or custom playback code`() = runBlocking {
        val source = EmbedMoviesProvider(policy.allowedHosts).getSeriesSource(30984, 1, 1)!!
        val html = EmbedPage.html(source, policy)
        assertTrue(html.contains("width=\"100%\" height=\"100%\""))
        assertTrue(html.contains("scrolling=\"no\""))
        assertTrue(html.contains("width=device-width, initial-scale=1"))
        assertTrue(html.contains("autoplay; encrypted-media; picture-in-picture; fullscreen"))
        assertFalse(html.contains("referrerpolicy=\"unsafe-url"))
        assertFalse(html.contains("<script"))
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
