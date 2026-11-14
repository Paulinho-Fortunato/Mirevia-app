package ao.mirevia.playback

import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewFeature
import ao.mirevia.core.playback.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class SecureEmbedWebViewTest {
    @Test
    fun restrictsSettingsWithoutContactingTheProvider() =
        withView(requireWorkerSupport = false) { view ->
            assertFalse(view.settings.allowFileAccess)
            assertFalse(view.settings.allowContentAccess)
            assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, view.settings.mixedContentMode)
            assertTrue(view.settings.mediaPlaybackRequiresUserGesture)
            assertFalse(CookieManager.getInstance().acceptThirdPartyCookies(view))
        }

    @Test
    fun unlistedHttpsResourcesAndFramesUseTheSamePolicyInWebViewAndWorkers() = withView { view ->
        listOf(
                "https://static.cloudflareinsights.com/beacon.min.js",
                "https://cloudflareinsights.com/cdn-cgi/rum",
                "https://not-on-any-list.example/asset",
                "https://cdn.myembed.biz/frame",
                "https://another.example:443/redirect",
            )
            .forEach { url ->
                val request = request(url)
                assertFalse(view.requestClient.shouldOverrideUrlLoading(view, request))
                assertNull(view.requestClient.shouldInterceptRequest(view, request))
                assertNull(view.workerClient.shouldInterceptRequest(request))
            }
        assertNull(view.url) // Calling the clients does not load a remote document.
    }

    @Test
    fun wrapperIsServedLocallyOnlyAsTheExactMainDocument() = withView { view ->
        val request = request(EmbedPage.URL, main = true)
        assertFalse(view.requestClient.shouldOverrideUrlLoading(view, request))
        val response = view.requestClient.shouldInterceptRequest(view, request)!!
        assertEquals("text/html", response.mimeType)
        val html = response.data.bufferedReader().use { it.readText() }
        assertTrue(html.contains("<iframe src=\"https://myembed.biz/filme/11\""))
        assertTrue(html.contains("frame-src https://*:443;"))
        assertFalse(html.contains("<script"))
        assertNull(view.url)
    }

    @Test
    fun evenTheOfficialIframeCannotReplaceTheMainDocument() = withView { view ->
        val request = request("https://myembed.biz/filme/11", main = true)
        assertTrue(view.requestClient.shouldOverrideUrlLoading(view, request))
        assertEquals(403, view.requestClient.shouldInterceptRequest(view, request)!!.statusCode)
    }

    @Test
    fun observableCleartextResourcesAreStillDenied() = withView { view ->
        val request = request("http://static.cloudflareinsights.com/beacon.min.js")
        assertEquals(403, view.requestClient.shouldInterceptRequest(view, request)!!.statusCode)
    }

    @Test
    fun workerCannotBypassTheHttpsRequirement() = withView { view ->
        val request = request("http://unlisted.example/worker-resource")
        assertEquals(403, view.workerClient.shouldInterceptRequest(request)!!.statusCode)
    }

    @Test
    fun aReleasedSessionDeniesEvenHttpsForBothRetainedClients() = withView { view ->
        view.release()
        val request = request("https://static.cloudflareinsights.com/beacon.min.js")
        assertEquals(403, view.requestClient.shouldInterceptRequest(view, request)!!.statusCode)
        assertEquals(403, view.workerClient.shouldInterceptRequest(request)!!.statusCode)
    }

    private fun withView(
        requireWorkerSupport: Boolean = true,
        block: (SecureEmbedWebView) -> Unit,
    ) {
        if (requireWorkerSupport)
            assumeTrue(
                "The player fails closed without worker interception; use an updated System WebView.",
                WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) &&
                    WebViewFeature.isFeatureSupported(
                        WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST
                    ),
            )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val source = runBlocking {
            EmbedMoviesProvider(setOf("myembed.biz")).getMovieSource(11, null, null, null)!!
        }
        instrumentation.runOnMainSync {
            val view =
                SecureEmbedWebView(
                    instrumentation.targetContext,
                    source,
                    EmbedUrlPolicy(setOf("myembed.biz")),
                    EmbedFullscreenController(),
                    0,
                    {},
                    {},
                    {},
                    autoLoad = false,
                )
            try {
                block(view)
            } finally {
                view.release()
            }
        }
    }

    private fun request(value: String, main: Boolean = false) =
        object : WebResourceRequest {
            override fun getUrl(): Uri = Uri.parse(value)

            override fun isForMainFrame() = main

            override fun isRedirect() = false

            override fun hasGesture() = false

            override fun getMethod() = "GET"

            override fun getRequestHeaders(): Map<String, String> = emptyMap()
        }
}
