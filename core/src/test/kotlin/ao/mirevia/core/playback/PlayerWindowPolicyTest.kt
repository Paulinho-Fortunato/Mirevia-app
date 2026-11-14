package ao.mirevia.core.playback

import ao.mirevia.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PlayerWindowPolicyTest {
    @Test
    fun `unlisted HTTPS windows and resources do not require host approval`() {
        listOf(
                "https://not-listed.example/page?opaque=value",
                "https://static.cloudflareinsights.com/beacon.min.js",
                "https://another.example:443/redirect",
                "https://cdn.myembed.biz/frame",
                "HTTPS://UNLISTED.EXAMPLE/page",
            )
            .forEach { assertTrue(it, PlayerWindowPolicy.isAllowedRequest(it)) }
    }

    @Test
    fun `a local blank bootstrap is not a remote or playback source`() {
        assertTrue(PlayerWindowPolicy.isAllowedRequest("about:blank"))
        assertFalse(IframeRequestPolicy.isAllowedRequest("about:blank", false))
        assertFalse(EmbedUrlPolicy(setOf("myembed.biz")).isOfficialEmbedUrl("about:blank"))
        listOf(
                "about:srcdoc",
                "about:blank#secret",
                "about:blank?secret",
                "ABOUT:blank",
                " about:blank",
            )
            .forEach { assertFalse(it, PlayerWindowPolicy.isAllowedRequest(it)) }
    }

    @Test
    fun `the same HTTPS target can navigate a popup but never replace the iframe main document`() {
        val url = "https://another.example/frame"
        assertTrue(PlayerWindowPolicy.isAllowedRequest(url))
        assertTrue(IframeRequestPolicy.isAllowedRequest(url, false))
        assertFalse(IframeRequestPolicy.isAllowedRequest(url, true))
        assertTrue(
            IframeRequestPolicy.isAllowedRequest(IframeRequestPolicy.LOCAL_DOCUMENT_URL, true)
        )
        assertFalse(PlayerWindowPolicy.isAllowedRequest(IframeRequestPolicy.LOCAL_DOCUMENT_URL))
    }

    @Test
    fun `no host list does not authorize dangerous schemes malformed URLs or alternate ports`() {
        listOf(
                "http://not-listed.example/page",
                "intent://not-listed.example/#Intent;scheme=https;end",
                "javascript:alert(1)",
                "file:///private",
                "content://private/a",
                "data:text/html,secret",
                "blob:https://myembed.biz/id",
                "tel:123",
                "market://details?id=a",
                "https://user:secret@not-listed.example/page",
                "https://not-listed.example:8443/page",
                "https://not-listed.example:/page",
                "https://not-listed.example\\@other.example/page",
                "https://not-listed.example/%xx",
                "https://not-listed.example/\nsecret",
                "https://${IframeRequestPolicy.LOCAL_HOST}./player/index.html",
                "",
                "not a URL",
            )
            .forEach {
                assertFalse(it, PlayerWindowPolicy.isAllowedRequest(it))
                assertFalse(it, IframeRequestPolicy.isAllowedRequest(it, false))
            }
    }

    @Test
    fun `a popup URL can never become an initial or shared video source`() = runBlocking {
        val policy = EmbedUrlPolicy(setOf("myembed.biz"))
        val media = MediaSummary(MediaKey(DataOrigin.TMDB, MediaType.MOVIE, 11), "Fixture")
        val official =
            EmbedMoviesProvider(policy.allowedHosts).getMovieSource(11, null, null, null)!!
        val url = "https://unlisted.example/filme/11"
        assertTrue(PlayerWindowPolicy.isAllowedRequest(url))
        assertFalse(policy.isValidSource(official.copy(url = url)))
        assertNull(MediaSharing.embed(media, official.copy(url = url), policy))
        assertNotNull(MediaSharing.embed(media, official, policy))
    }

    @Test
    fun `resource-only consent revision two does not authorize windows`() {
        assertEquals(3, IframeRequestPolicy.CONSENT_REVISION)
        assertFalse(IframeRequestPolicy.hasCurrentConsent(true, 2))
        assertFalse(IframeRequestPolicy.hasCurrentConsent(false, 3))
        assertTrue(IframeRequestPolicy.hasCurrentConsent(true, 3))
    }
}
