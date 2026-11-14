package ao.mirevia.core.playback

import org.junit.Assert.*
import org.junit.Test

class EmbedUrlPolicyTest {
    private val policy = EmbedUrlPolicy(setOf("embedmovies.org", "myembed.biz"))

    @Test
    fun `allows exact HTTPS origins`() {
        assertTrue(policy.isAllowedNavigation("https://myembed.biz/filme/11"))
        assertTrue(policy.isAllowedNavigation("https://embedmovies.org/"))
        assertTrue(policy.isAllowedNavigation("https://MYEMBED.BIZ/filme/11"))
    }

    @Test
    fun `rejects lookalikes subdomains and dangerous authorities`() {
        val urls =
            listOf(
                "https://embedmovies.org.example.com/",
                "https://embedmovies-org.com/",
                "https://embedmovies.org.attacker.com/",
                "https://sub.myembed.biz/",
                "https://myembed.biz@evil.example/",
                "https://evil@myembed.biz/",
                "https://myembed.biz:8443/",
                "https://myembed.biz./",
                "https://myembed.biz%2eevil.example/",
                "https://myembed.biz\\@evil.example/",
                "https://127.0.0.1/",
                "https://[::1]/",
                "https://myembed.biz\n/",
                " https://myembed.biz/",
                "https://myembed.biz/ ",
                "",
                "not a url",
            )
        for (url in urls) assertFalse(url, policy.isAllowedNavigation(url))
    }

    @Test
    fun `rejects non HTTPS schemes`() {
        for (url in
            listOf(
                "http://myembed.biz/",
                "javascript:alert(1)",
                "file:///data/a",
                "content://myembed.biz/",
                "intent://myembed.biz/",
                "data:text/html,test",
                "//myembed.biz/",
            )) {
            assertFalse(url, policy.isAllowedNavigation(url))
        }
    }

    @Test
    fun `only documented source paths are allowed`() {
        for (url in
            listOf(
                "https://myembed.biz/filme/11",
                "https://myembed.biz/filme/tt0076759",
                "https://myembed.biz/serie/30984",
                "https://myembed.biz/serie/30984/1/2",
            )) {
            assertTrue(url, policy.isOfficialEmbedUrl(url))
        }
        for (path in
            listOf(
                "/filme/0",
                "/filme/-1",
                "/filme/11/",
                "/serie/1/2",
                "/serie/1/0/1",
                "/filme/%31%31",
                "/filme/11?url=bad",
                "/filme/11#test",
                "/video.mp4",
                "/filme/../filme/11",
            )) {
            assertFalse(path, policy.isOfficialEmbedUrl("https://myembed.biz$path"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `wildcard host cannot enter allowlist`() {
        EmbedUrlPolicy(setOf("*.myembed.biz"))
    }
}
