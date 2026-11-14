package ao.mirevia.playback

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * Shared native settings for the iframe and its transported windows; no scripts or URL rewriting.
 */
@SuppressLint("SetJavaScriptEnabled")
@Suppress("DEPRECATION")
internal fun WebView.configurePlayerBrowser() {
    setBackgroundColor(android.graphics.Color.BLACK)
    WebView.setWebContentsDebuggingEnabled(false)
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true // Provider storage; final session cleanup is owned by the iframe.
        allowFileAccess = false
        allowContentAccess = false
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        mediaPlaybackRequiresUserGesture = true
        javaScriptCanOpenWindowsAutomatically = false
        setSupportMultipleWindows(true)
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
        cacheMode = WebSettings.LOAD_NO_CACHE
        setSaveFormData(false)
        defaultTextEncodingName = "UTF-8"
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE))
            WebSettingsCompat.setSafeBrowsingEnabled(this, true)
    }
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(this@configurePlayerBrowser, false)
    }
}
