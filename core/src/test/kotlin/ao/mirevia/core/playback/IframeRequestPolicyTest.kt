package ao.mirevia.core.playback

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class IframeRequestPolicyTest {
    @Test
    fun `cloudflare analytics and unlisted HTTPS resources are accepted without a host list`() {
        listOf(
                "https://static.cloudflareinsights.com/beacon.min.js",
                "https://cloudflareinsights.com/cdn-cgi/rum",
                "https://unlisted.example/a?opaque=value#fragment",
                "https://another.example:443/resource",
                "https://cdn.myembed.biz/image",
                "https://MYEMBED.BIZ/serie/30984/1/1",
                "HTTPS://UNLISTED.EXAMPLE/a",
                "https://xn--exemplo-2ya.example/asset",
            )
            .forEach { url -> assertTrue(url, IframeRequestPolicy.isAllowedRequest(url, false)) }
    }

    @Test
    fun `main document can never leave the exact locally served wrapper`() {
        assertTrue(
            IframeRequestPolicy.isAllowedRequest(IframeRequestPolicy.LOCAL_DOCUMENT_URL, true)
        )
        listOf(
                "https://myembed.biz/filme/969681",
                "https://static.cloudflareinsights.com/beacon.min.js",
                "https://unlisted.example/redirect",
                IframeRequestPolicy.LOCAL_DOCUMENT_URL + "?redirect=1",
                IframeRequestPolicy.LOCAL_DOCUMENT_URL + "#fragment",
                "https://${IframeRequestPolicy.LOCAL_HOST}:443/player/index.html",
                "https://${IframeRequestPolicy.LOCAL_HOST}/player/other.html",
                "https://${IframeRequestPolicy.LOCAL_HOST}.example/player/index.html",
                "http://${IframeRequestPolicy.LOCAL_HOST}/player/index.html",
                "about:blank",
                "",
            )
            .forEach { url -> assertFalse(url, IframeRequestPolicy.isAllowedRequest(url, true)) }
    }

    @Test
    fun `reserved wrapper origin is never fetched as an iframe resource`() {
        listOf(
                IframeRequestPolicy.LOCAL_DOCUMENT_URL,
                "https://${IframeRequestPolicy.LOCAL_HOST}/anything",
                "https://${IframeRequestPolicy.LOCAL_HOST}:443/player/index.html",
                "https://${IframeRequestPolicy.LOCAL_HOST}./player/index.html",
                "https://APPASSETS.ANDROIDPLATFORM.NET/player/index.html",
            )
            .forEach { url -> assertFalse(url, IframeRequestPolicy.isAllowedRequest(url, false)) }
    }

    @Test
    fun `HTTP and dangerous custom or file schemes remain denied`() {
        listOf(
                "http://static.cloudflareinsights.com/beacon.min.js",
                "http://myembed.biz/filme/969681",
                "javascript:alert(1)",
                "file:///data/user/0/private",
                "content://example/private",
                "intent://example/#Intent;scheme=https;end",
                "data:text/html,secret",
                "blob:https://myembed.biz/id",
                "ftp://example/file",
                "wss://example/socket",
                "about:blank",
                "//unlisted.example/resource",
            )
            .forEach { url -> assertFalse(url, IframeRequestPolicy.isAllowedRequest(url, false)) }
    }

    @Test
    fun `alternate ports credentials and malformed authorities remain denied`() {
        listOf(
                "https://unlisted.example:80/a",
                "https://unlisted.example:8443/a",
                "https://unlisted.example:0/a",
                "https://unlisted.example:65536/a",
                "https://unlisted.example:/a",
                "https://unlisted.example:0443/a",
                "https://user:secret@unlisted.example/a",
                "https://myembed.biz@unlisted.example/a",
                "https://unlisted.example%2eother.example/a",
                "https://unlisted.example%40other.example/a",
                "https://unlisted.example\\@other.example/a",
                "https:///unlisted.example/a",
                "https:unlisted.example/a",
                "https://[invalid]/a",
                "https://unlisted.example/%xx",
                "https://unlisted.example/\nsecret",
                "https://unlisted.example/\tsecret",
                "https://unlisted.example/\u007fsecret",
                " https://unlisted.example/a",
                "https://unlisted.example/a ",
                "not a URL",
                "",
            )
            .forEach { url -> assertFalse(url, IframeRequestPolicy.isAllowedRequest(url, false)) }
    }

    @Test
    fun `resource permission never expands the initial source or share URL policy`() = runBlocking {
        val sourcePolicy = EmbedUrlPolicy(setOf(EmbedMoviesUrlBuilder.DOCUMENTED_HOST))
        val source =
            EmbedMoviesProvider(sourcePolicy.allowedHosts).getMovieSource(11, null, null, null)!!
        assertTrue(sourcePolicy.isValidSource(source))
        listOf(
                "https://static.cloudflareinsights.com/filme/11",
                "https://unlisted.example/filme/11",
                "https://cdn.myembed.biz/filme/11",
                "https://myembed.biz.lookalike.example/filme/11",
            )
            .forEach { url ->
                assertTrue(url, IframeRequestPolicy.isAllowedRequest(url, false))
                assertFalse(url, sourcePolicy.isOfficialEmbedUrl(url))
                assertFalse(url, sourcePolicy.isValidSource(source.copy(url = url)))
                assertFalse(url, IframeRequestPolicy.isAllowedRequest(url, true))
            }
        assertEquals(setOf("myembed.biz"), sourcePolicy.allowedHosts)
    }

    @Test
    fun `legacy unknown and future consent revisions cannot authorize third parties`() {
        listOf(null, -1, 0, 1, IframeRequestPolicy.CONSENT_REVISION + 1).forEach { revision ->
            assertFalse("revision=$revision", IframeRequestPolicy.hasCurrentConsent(true, revision))
        }
        assertTrue(
            IframeRequestPolicy.hasCurrentConsent(true, IframeRequestPolicy.CONSENT_REVISION)
        )
    }

    @Test
    fun `revocation denies consent regardless of the stored revision`() {
        listOf(null, 1, IframeRequestPolicy.CONSENT_REVISION, Int.MAX_VALUE).forEach { revision ->
            assertFalse(IframeRequestPolicy.hasCurrentConsent(false, revision))
        }
    }
}
