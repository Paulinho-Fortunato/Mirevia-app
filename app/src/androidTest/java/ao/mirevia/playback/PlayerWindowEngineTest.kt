package ao.mirevia.playback

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import ao.mirevia.ui.player.PlayerWindowsOverlay
import ao.mirevia.ui.theme.MireviaTheme
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Real engine-created window/transport/opener, using only this test's local HTML. The script is
 * part of an owned fixture, never injected into the external provider. No TMDB/player URL is
 * loaded.
 */
class PlayerWindowEngineTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var windows: PlayerWindowController
    private lateinit var opener: WebView
    private val ready = AtomicBoolean(false)
    private val notices = mutableListOf<String>()

    @Before
    fun setUp() {
        compose.setContent {
            val owner = remember { Any() }
            val controller = remember { PlayerWindowController { notices += it } }
            SideEffect { windows = controller }
            MireviaTheme {
                Box(Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                opener = this
                                configurePlayerBrowser()
                                webViewClient =
                                    object : WebViewClient() {
                                        override fun onPageFinished(view: WebView, url: String?) {
                                            ready.set(true)
                                        }

                                        // The local fixture has no external dependencies. Any
                                        // unexpected
                                        // observable network request is refused, never forwarded.
                                        override fun shouldInterceptRequest(
                                            view: WebView,
                                            request: WebResourceRequest,
                                        ): WebResourceResponse? =
                                            if (
                                                request.url.scheme == "data" ||
                                                    request.url.toString() == "about:blank"
                                            )
                                                null
                                            else
                                                WebResourceResponse(
                                                    "text/plain",
                                                    "UTF-8",
                                                    403,
                                                    "Forbidden",
                                                    emptyMap(),
                                                    ByteArrayInputStream(ByteArray(0)),
                                                )
                                    }
                                webChromeClient =
                                    object : WebChromeClient() {
                                        override fun onCreateWindow(
                                            view: WebView,
                                            isDialog: Boolean,
                                            isUserGesture: Boolean,
                                            resultMsg: android.os.Message,
                                        ): Boolean =
                                            controller.open(context, owner, null, resultMsg)
                                    }
                                loadDataWithBaseURL(null, LOCAL_FIXTURE, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize().testTag("local-window-opener"),
                        onRelease = { view ->
                            controller.closeOwner(owner)
                            (view.parent as? ViewGroup)?.removeView(view)
                            view.stopLoading()
                            view.webChromeClient = null
                            view.webViewClient = WebViewClient()
                            view.destroy()
                        },
                    )
                    PlayerWindowsOverlay(controller)
                }
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) { ready.get() }
    }

    @After
    fun releaseWindows() {
        if (::windows.isInitialized) compose.runOnIdle { windows.closeAll() }
    }

    @Test
    fun realGestureTransportsWindowPreservesOpenerAndCanReopenWithoutReloadingIt() {
        repeat(2) { index ->
            // The fixture's button fills the entire WebView. This is an actual input gesture, not
            // evaluateJavascript, a fake Message, an automatic popup or a simulated provider click.
            compose.onNodeWithTag("local-window-opener").performTouchInput { click(center) }
            compose.waitUntil(timeoutMillis = 10_000) {
                compose.runOnIdle {
                    windows.activeWindow?.value?.browser?.title == "Opener preserved"
                }
            }
            compose.runOnIdle {
                assertTrue(windows.activeWindow!!.value.browser.isAttachedToWindow)
                assertEquals("Local fixture opened ${index + 1}", opener.title)
                assertTrue(notices.isEmpty())
            }
            compose.onNodeWithText("Fechar").assertIsDisplayed().performClick()
            compose.runOnIdle {
                assertNull(windows.activeWindow)
                assertEquals("Local fixture opened ${index + 1}", opener.title)
            }
        }
    }

    companion object {
        private const val LOCAL_FIXTURE =
            """<!doctype html>
            <html><head><meta name="viewport" content="width=device-width,initial-scale=1">
            <title>Local fixture ready</title></head><body>
            <button style="position:fixed;top:0;left:0;right:0;bottom:0;width:100%;height:100%" onclick="openLocal()">
            Open local fixture</button>
            <script>
              let openings = 0;
              function openLocal() {
                const popup = window.open('about:blank', '_blank');
                if (popup) {
                  openings += 1;
                  document.title = 'Local fixture opened ' + openings;
                  popup.document.title = popup.opener === window ? 'Opener preserved' : 'Opener missing';
                }
              }
            </script></body></html>"""
    }
}
