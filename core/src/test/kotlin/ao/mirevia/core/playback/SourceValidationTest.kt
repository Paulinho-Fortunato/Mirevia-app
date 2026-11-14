package ao.mirevia.core.playback

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SourceValidationTest {
    private val policy = EmbedUrlPolicy(setOf("myembed.biz"))

    @Test
    fun `validates source flags rather than trusting a URL alone`() = runBlocking {
        val source = EmbedMoviesProvider(policy.allowedHosts).getMovieSource(11, null, null, null)!!
        assertFalse(policy.isValidSource(source.copy(isIframe = false)))
        assertFalse(policy.isValidSource(source.copy(requiresWebView = false)))
        assertFalse(
            policy.isValidSource(source.copy(subtitlesUrl = "https://example.com/subtitle.vtt"))
        )
        assertTrue(policy.isValidSource(source))
    }

    @Test
    fun `overflow IDs encoded paths and zero IMDb are rejected`() {
        for (path in
            listOf(
                "/filme/9999999999",
                "/serie/2147483648",
                "/serie/1/2147483648/1",
                "/filme/tt0000000",
                "/filme/%2f11",
                "/serie/1/1/%31",
            )) {
            assertFalse(path, policy.isOfficialEmbedUrl("https://myembed.biz$path"))
        }
        assertTrue(policy.isOfficialEmbedUrl("https://myembed.biz/filme/2147483647"))
    }

    @Test
    fun `never accepts host suffix attacks across many variants`() {
        val suffixes =
            listOf(
                ".example.com",
                "-attacker.com",
                ".attacker.org",
                ".localhost",
                ".evil",
                "@attacker.org",
            )
        for (suffix in suffixes) {
            assertFalse(policy.isAllowedNavigation("https://myembed.biz$suffix/filme/11"))
        }
    }
}
