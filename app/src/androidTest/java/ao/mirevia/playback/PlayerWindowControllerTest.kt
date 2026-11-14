package ao.mirevia.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.webkit.WebViewFeature
import ao.mirevia.core.playback.*
import ao.mirevia.ui.player.PlayerWindowsOverlay
import ao.mirevia.ui.theme.MireviaTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Real native views and transport objects, but fake response Handler and no loadUrl/provider
 * traffic.
 */
class PlayerWindowControllerTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private lateinit var windows: PlayerWindowController
    private lateinit var root: SecureEmbedWebView
    private lateinit var fullscreen: EmbedFullscreenController
    private val notices = mutableListOf<String>()
    private var failures = 0
    private var blocks = 0
    private var delivered = 0
    private var deliveredView: WebView? = null

    @Before
    fun setUp() {
        assumeTrue(
            WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) &&
                WebViewFeature.isFeatureSupported(
                    WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST
                )
        )
        compose.setContent {
            val uiContext = LocalContext.current
            val controller = remember { PlayerWindowController { notices += it } }
            SideEffect {
                context = uiContext
                windows = controller
            }
            MireviaTheme { PlayerWindowsOverlay(controller) }
        }
        val source = runBlocking {
            EmbedMoviesProvider(setOf("myembed.biz")).getMovieSource(11, null, null, null)!!
        }
        compose.runOnIdle {
            fullscreen = EmbedFullscreenController()
            root =
                SecureEmbedWebView(
                    context,
                    source,
                    EmbedUrlPolicy(setOf("myembed.biz")),
                    fullscreen,
                    windows,
                    0,
                    {},
                    { failures++ },
                    { blocks++ },
                    autoLoad = false,
                )
        }
    }

    @After
    fun release() {
        if (::windows.isInitialized)
            compose.runOnIdle {
                if (::root.isInitialized) root.release()
                windows.closeAll()
            }
    }

    @Test
    fun attachesFreshWindowBeforeDeliveringTransportWithoutBlockingTheIframe() {
        lateinit var transport: WebView.WebViewTransport
        compose.runOnIdle {
            transport = root.WebViewTransport()
            val message = responseMessage(transport)
            assertTrue(root.chromeClient.onCreateWindow(root, false, true, message))
            assertNull(transport.webView) // Still unnavigated/unattached during the callback.
            assertNotNull(windows.activeWindow)
            assertIframeStillUsable()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertSame(windows.activeWindow!!.value.browser, transport.webView)
            assertTrue(transport.webView!!.isAttachedToWindow)
            assertTrue(
                transport.webView!!.url == null ||
                    transport.webView!!.url == PlayerWindowPolicy.BLANK_DOCUMENT
            )
            assertEquals(1, delivered)
            assertSame(transport.webView, deliveredView)
            assertIframeStillUsable()
        }
        compose.onNodeWithText("Janela do player · terceiros").assertIsDisplayed()
        compose.onNodeWithText("Fechar").assertIsDisplayed()
    }

    @Test
    fun closeButtonDestroysOnlyTheWindowAndLeavesTheOriginalIframeAlive() {
        openRootWindow()
        compose.onNodeWithText("Fechar").performClick()
        compose.runOnIdle {
            assertNull(windows.activeWindow)
            assertIframeStillUsable()
        }
    }

    @Test
    fun androidBackClosesTheModalWindowInsteadOfLeavingThePlayer() {
        openRootWindow()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.runOnIdle {
            assertNull(windows.activeWindow)
            assertIframeStillUsable()
        }
        compose.onNodeWithText("Janela do player · terceiros").assertDoesNotExist()
    }

    @Test
    fun openingAWindowExitsTheIframeFullscreenSoCloseIsVisible() {
        var hidden = 0
        compose.runOnIdle {
            root.chromeClient.onShowCustomView(
                View(context),
                WebChromeClient.CustomViewCallback { hidden++ },
            )
            assertNotNull(fullscreen.customView)
        }
        openRootWindow()
        compose.runOnIdle {
            assertNull(fullscreen.customView)
            assertEquals(1, hidden)
            assertIframeStillUsable()
        }
        compose.onNodeWithText("Fechar").assertIsDisplayed()
    }

    @Test
    fun onlyOneTransportCanBePendingForTheSameOpener() {
        lateinit var accepted: WebView.WebViewTransport
        lateinit var refused: WebView.WebViewTransport
        compose.runOnIdle {
            accepted = root.WebViewTransport()
            refused = root.WebViewTransport()
            assertTrue(
                root.chromeClient.onCreateWindow(root, false, true, responseMessage(accepted))
            )
            assertFalse(
                root.chromeClient.onCreateWindow(root, false, true, responseMessage(refused))
            )
            assertIframeStillUsable()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertNotNull(accepted.webView)
            assertNull(refused.webView)
            assertEquals(1, delivered)
            assertEquals(1, notices.size)
        }
    }

    @Test
    fun fourthWindowIsRefusedWithoutEndingAnyExistingWindowOrIframe() {
        repeat(PlayerWindowStack.MAX_WINDOWS) { openRootWindow() }
        compose.runOnIdle {
            val previous = windows.activeWindow
            val refused = root.WebViewTransport()
            assertFalse(
                root.chromeClient.onCreateWindow(root, false, true, responseMessage(refused))
            )
            assertSame(previous, windows.activeWindow)
            assertNull(refused.webView)
            assertEquals(3, delivered)
            assertEquals(1, notices.size)
            assertIframeStillUsable()
        }
    }

    @Test
    fun providerOnCloseWindowDoesNotCloseTheIframe() {
        openRootWindow()
        compose.runOnIdle {
            val child = windows.activeWindow!!.value.browser
            child.chromeClient.onCloseWindow(child)
            assertNull(windows.activeWindow)
            assertIframeStillUsable()
        }
    }

    @Test
    fun closingNestedWindowReattachesTheSameUnreleasedOpener() {
        openRootWindow()
        lateinit var opener: PlayerWindowWebView
        compose.runOnIdle {
            opener = windows.activeWindow!!.value.browser
            assertTrue(
                opener.chromeClient.onCreateWindow(
                    opener,
                    false,
                    true,
                    responseMessage(opener.WebViewTransport()),
                )
            )
        }
        compose.waitForIdle()
        compose.onNodeWithText("Fechar").performClick()
        compose.runOnIdle {
            assertSame(opener, windows.activeWindow!!.value.browser)
            assertTrue(opener.isAttachedToWindow)
            assertNull(
                opener.requestClient.shouldInterceptRequest(
                    opener,
                    request("https://unlisted.example/frame"),
                )
            )
            assertIframeStillUsable()
        }
    }

    @Test
    fun rejectedSchemeFailsTheWindowNotThePlayerSession() {
        openRootWindow()
        compose.runOnIdle {
            val child = windows.activeWindow!!.value.browser
            assertTrue(
                child.requestClient.shouldOverrideUrlLoading(
                    child,
                    request("intent://example/action", main = true),
                )
            )
        }
        compose.waitForIdle()
        compose.onNodeWithText("Não foi possível abrir esta janela").assertExists()
        compose.runOnIdle {
            assertNotNull(windows.activeWindow!!.value.failure)
            assertIframeStillUsable()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun safeBrowsingStopsOnlyTheWindowWithoutCallingProceed() {
        openRootWindow()
        var backedOut = false
        val response =
            object : SafeBrowsingResponse() {
                override fun backToSafety(report: Boolean) {
                    backedOut = true
                }

                override fun proceed(report: Boolean) {
                    fail("Safe Browsing must never be bypassed")
                }

                override fun showInterstitial(allowReporting: Boolean) {
                    fail("Expected backToSafety")
                }
            }
        compose.runOnIdle {
            val child = windows.activeWindow!!.value.browser
            child.requestClient.onSafeBrowsingHit(
                child,
                request("https://unlisted.example/unsafe"),
                WebViewClient.SAFE_BROWSING_THREAT_MALWARE,
                response,
            )
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(backedOut)
            assertNotNull(windows.activeWindow!!.value.failure)
            assertIframeStillUsable()
        }
    }

    @Test
    fun windowSettingsKeepFileCookieAndMixedContentRestrictions() {
        openRootWindow()
        compose.runOnIdle {
            val child = windows.activeWindow!!.value.browser
            assertFalse(child.settings.allowFileAccess)
            assertFalse(child.settings.allowContentAccess)
            assertFalse(child.settings.javaScriptCanOpenWindowsAutomatically)
            assertTrue(child.settings.mediaPlaybackRequiresUserGesture)
            assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, child.settings.mixedContentMode)
            assertFalse(CookieManager.getInstance().acceptThirdPartyCookies(child))
            val request = request("https://never-listed.example/window", main = true)
            assertFalse(child.requestClient.shouldOverrideUrlLoading(child, request))
            assertNull(child.requestClient.shouldInterceptRequest(child, request))
            assertIframeStillUsable()
        }
    }

    @Test
    fun invalidTransportNeverSendsAMessageOrMakesNewWindowFatal() {
        compose.runOnIdle {
            val message =
                Message.obtain(
                    Handler(Looper.getMainLooper()) {
                        delivered++
                        true
                    },
                    1,
                    "not a transport",
                )
            assertFalse(root.chromeClient.onCreateWindow(root, false, true, message))
            assertNull(windows.activeWindow)
            assertEquals(0, delivered)
            assertEquals(1, notices.size)
            assertIframeStillUsable()
        }
    }

    @Test
    fun releasingOwnerCancelsPendingTransportAndClosesAllItsWindows() {
        lateinit var transport: WebView.WebViewTransport
        compose.runOnIdle {
            transport = root.WebViewTransport()
            assertTrue(
                root.chromeClient.onCreateWindow(root, false, true, responseMessage(transport))
            )
            root.release() // Close before Compose can attach or fetch anything.
            assertNull(windows.activeWindow)
            assertNull(transport.webView)
            assertFalse(
                root.chromeClient.onCreateWindow(
                    root,
                    false,
                    true,
                    responseMessage(root.WebViewTransport()),
                )
            )
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, delivered) // One null response cancels the accepted native request.
            assertNull(deliveredView)
        }
    }

    @Test
    fun windowOriginCallbacksNeverRetainQueryOrPathInNativeChrome() {
        openRootWindow()
        compose.runOnIdle {
            val child = windows.activeWindow!!.value.browser
            child.requestClient.onPageStarted(
                child,
                "https://initial.example/redirect?token=initial-secret",
                null,
            )
            child.requestClient.onPageCommitVisible(
                child,
                "https://third-party.example/private/secret?token=secret-token",
            )
            child.requestClient.onPageFinished(
                child,
                "https://third-party.example/private/secret?token=secret-token",
            )
        }
        compose.onNodeWithText("Origem: https://third-party.example").assertIsDisplayed()
        compose.onNodeWithText("secret-token", substring = true).assertDoesNotExist()
        compose.onNodeWithText("/private", substring = true).assertDoesNotExist()
        compose.onNodeWithText("initial.example", substring = true).assertDoesNotExist()
    }

    private fun openRootWindow() {
        compose.runOnIdle {
            assertTrue(
                root.chromeClient.onCreateWindow(
                    root,
                    false,
                    true,
                    responseMessage(root.WebViewTransport()),
                )
            )
        }
        compose.waitForIdle()
    }

    private fun responseMessage(transport: WebView.WebViewTransport): Message {
        val handler =
            Handler(Looper.getMainLooper()) { response ->
                delivered++
                deliveredView = (response.obj as WebView.WebViewTransport).webView
                true
            }
        return Message.obtain(handler, 1, transport)
    }

    private fun assertIframeStillUsable() {
        assertEquals(0, failures)
        assertEquals(0, blocks)
        assertNull(
            root.requestClient.shouldInterceptRequest(root, request("https://myembed.biz/filme/11"))
        )
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
