package ao.mirevia.playback

import android.webkit.CookieManager
import android.webkit.WebSettings
import androidx.test.platform.app.InstrumentationRegistry
import ao.mirevia.core.playback.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SecureEmbedWebViewTest {
    @Test
    fun restrictsSettingsWithoutContactingTheProvider() {
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
                assertFalse(view.settings.allowFileAccess)
                assertFalse(view.settings.allowContentAccess)
                assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, view.settings.mixedContentMode)
                assertTrue(view.settings.mediaPlaybackRequiresUserGesture)
                assertFalse(CookieManager.getInstance().acceptThirdPartyCookies(view))
            } finally {
                view.release()
            }
        }
    }
}
