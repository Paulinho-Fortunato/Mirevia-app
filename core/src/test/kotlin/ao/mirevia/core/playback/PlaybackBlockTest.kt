package ao.mirevia.core.playback

import org.junit.Assert.*
import org.junit.Test

class PlaybackBlockTest {
    private val policy = EmbedUrlPolicy(setOf("myembed.biz"))

    @Test
    fun `diagnostic retains only origin metadata and never the resource URL`() {
        val block =
            PlaybackBlock.request(
                PlaybackBlockKind.RESOURCE,
                "https://user:secret@cdn.example:8443/private/video.m3u8?token=private-token#session",
            )
        assertEquals("cdn.example", block.host)
        assertEquals("https", block.scheme)
        assertEquals(8443, block.port)
        val report = block.report(policy)
        listOf("user", "secret", "/private", "m3u8", "private-token", "session").forEach {
            assertFalse("Leaked $it", report.contains(it))
        }
        assertTrue(report.contains("Iframe inicial autorizado: myembed.biz"))
        assertTrue(report.contains(IframeRequestPolicy.RESOURCE_DESCRIPTION))
        assertFalse(report.contains("Domínios autorizados:"))
        assertFalse(policy.isAllowedNavigation("https://cdn.example"))
    }

    @Test
    fun `malformed URLs cannot inject new lines or leak their raw input`() {
        listOf(
                "https://cdn.example/\nTOKEN_LEAK",
                "https://cdn.example/\\TOKEN_LEAK",
                "not a URL TOKEN_LEAK",
                "https://cdn.example/%xxTOKEN_LEAK",
                "https://cdn.example/" + "TOKEN_LEAK".repeat(1000),
            )
            .forEach { url ->
                val block = PlaybackBlock.request(PlaybackBlockKind.RESOURCE, url)
                assertNull(block.host)
                assertNull(block.scheme)
                assertFalse(block.report(policy).contains("TOKEN_LEAK"))
            }
    }

    @Test
    fun `unsafe schemes are displayed only as scheme names`() {
        val block = PlaybackBlock.request(PlaybackBlockKind.NAVIGATION, "javascript:secretCode()")
        assertEquals("javascript", block.scheme)
        assertNull(block.host)
        assertFalse(block.report(policy).contains("secretCode"))
    }

    @Test
    fun `host case is normalized without trusting lookalikes or subdomains`() {
        val block =
            PlaybackBlock.request(PlaybackBlockKind.RESOURCE, "https://MYEMBED.BIZ.evil.example/a")
        assertEquals("myembed.biz.evil.example", block.host)
        assertFalse(policy.isAllowedNavigation("https://${block.host}"))
        assertFalse(policy.isAllowedNavigation("https://cdn.myembed.biz/a"))
        assertEquals(setOf("myembed.biz"), policy.allowedHosts)
    }

    @Test
    fun `cleartext and unusual ports remain blocked and diagnosable`() {
        val url = "http://myembed.biz:8080/filme/969681"
        val block = PlaybackBlock.request(PlaybackBlockKind.RESOURCE, url)
        assertEquals("http", block.scheme)
        assertEquals(8080, block.port)
        assertFalse(policy.isAllowedNavigation(url))
    }

    @Test
    fun `local blocks do not invent a host`() {
        PlaybackBlockKind.entries.forEach {
            val block = PlaybackBlock.local(it)
            assertEquals(it, block.kind)
            assertNull(block.host)
            assertNull(block.scheme)
            assertNull(block.port)
        }
    }
}
