package ao.mirevia.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.view.View
import android.webkit.*
import androidx.webkit.*
import ao.mirevia.core.playback.EmbedUrlPolicy
import ao.mirevia.core.playback.IframeRequestPolicy
import ao.mirevia.core.playback.PlaybackBlock
import ao.mirevia.core.playback.PlaybackBlockKind
import ao.mirevia.core.playback.VideoSource
import java.io.ByteArrayInputStream

@Suppress("DEPRECATION")
class SecureEmbedWebView(
    context: Context,
    private val source: VideoSource,
    private val policy: EmbedUrlPolicy,
    private val fullscreen: EmbedFullscreenController,
    private val windows: PlayerWindowController,
    val sessionToken: Long,
    private val onWrapperLoaded: () -> Unit,
    private val onFailure: () -> Unit,
    private val onSecurityBlocked: (PlaybackBlock) -> Unit,
    autoLoad: Boolean = true,
) : WebView(context) {
    // Retained for deterministic callback tests even on API 24 (WebView's getter is API 26+).
    internal val requestClient: WebViewClient
    internal val chromeClient: WebChromeClient
    private val windowOwner = Any()
    internal val workerClient =
        object : ServiceWorkerClientCompat() {
            override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? =
                filterWorkerRequest(request)
        }
    @Volatile private var released = false
    private var rendererGone = false
    private val securityFailureReported = java.util.concurrent.atomic.AtomicBoolean(false)
    private val document = EmbedPage.html(source, policy).toByteArray(Charsets.UTF_8)
    private val assetLoader =
        WebViewAssetLoader.Builder()
            .addPathHandler("/player/") { path ->
                if (path != "index.html") deniedResponse()
                else
                    WebResourceResponse("text/html", "UTF-8", ByteArrayInputStream(document))
                        .apply {
                            responseHeaders =
                                mapOf(
                                    "Cache-Control" to "no-store",
                                    "X-Content-Type-Options" to "nosniff",
                                    "Referrer-Policy" to "strict-origin-when-cross-origin",
                                )
                        }
            }
            .build()

    init {
        require(policy.isValidSource(source))
        configurePlayerBrowser()
        configureServiceWorkers()
        setDownloadListener { _, _, _, _, _ ->
            block(PlaybackBlock.local(PlaybackBlockKind.DOWNLOAD))
        }
        requestClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val url = request.url.toString()
                    val allowed = IframeRequestPolicy.isAllowedRequest(url, request.isForMainFrame)
                    if (!allowed) block(PlaybackBlock.request(PlaybackBlockKind.NAVIGATION, url))
                    return !allowed
                }

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = filterRequest(request)

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    if (url != EmbedPage.URL) {
                        stopLoading()
                        block(PlaybackBlock.request(PlaybackBlockKind.NAVIGATION, url))
                    }
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    if (!released && !securityFailureReported.get() && url == EmbedPage.URL)
                        onWrapperLoaded()
                    // This is a wrapper-document signal, NOT proof that a video started or
                    // completed.
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame || request.url.toString() == source.url) onFailure()
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse,
                ) {
                    if (
                        (request.isForMainFrame || request.url.toString() == source.url) &&
                            errorResponse.statusCode >= 400
                    )
                        onFailure()
                }

                override fun onReceivedSslError(
                    view: WebView,
                    handler: SslErrorHandler,
                    error: SslError,
                ) {
                    handler.cancel()
                    onFailure()
                }

                @androidx.annotation.RequiresApi(27)
                override fun onSafeBrowsingHit(
                    view: WebView,
                    request: WebResourceRequest,
                    threatType: Int,
                    callback: SafeBrowsingResponse,
                ) {
                    callback.backToSafety(true)
                    block(
                        PlaybackBlock.request(PlaybackBlockKind.UNSAFE_PAGE, request.url.toString())
                    )
                }

                @androidx.annotation.RequiresApi(26)
                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail,
                ): Boolean {
                    rendererGone = true
                    onFailure()
                    return true
                }
            }
        webViewClient = requestClient
        chromeClient =
            object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    if (source.supportsFullscreen && !released) fullscreen.show(view, callback)
                    else callback.onCustomViewHidden()
                }

                override fun onHideCustomView() {
                    fullscreen.hide()
                }

                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message,
                ): Boolean {
                    if (released || securityFailureReported.get()) return false
                    val opened = windows.open(context, windowOwner, null, resultMsg)
                    if (opened) fullscreen.hide()
                    return opened
                }

                override fun onCloseWindow(window: WebView) {
                    if (!released) windows.closeView(windowOwner, window)
                }

                override fun onPermissionRequest(request: PermissionRequest) {
                    request.deny()
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: GeolocationPermissions.Callback,
                ) {
                    callback.invoke(origin, false, false)
                }

                override fun onShowFileChooser(
                    webView: WebView,
                    filePathCallback: android.webkit.ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams,
                ): Boolean {
                    filePathCallback.onReceiveValue(null)
                    return true
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean =
                    true // No player URLs, tokens or cookies in logs.
            }
        webChromeClient = chromeClient
        if (autoLoad && !securityFailureReported.get()) loadUrl(EmbedPage.URL)
    }

    private fun filterRequest(request: WebResourceRequest): WebResourceResponse? {
        if (released || securityFailureReported.get()) return deniedResponse()
        val url = request.url.toString()
        if (!IframeRequestPolicy.isAllowedRequest(url, request.isForMainFrame)) {
            val diagnostic =
                if (request.isForMainFrame) PlaybackBlock.request(PlaybackBlockKind.NAVIGATION, url)
                else PlaybackBlock.request(PlaybackBlockKind.RESOURCE, url)
            block(diagnostic)
            return deniedResponse()
        }
        if (request.isForMainFrame)
            return assetLoader.shouldInterceptRequest(request.url) ?: deniedResponse()
        // Consented remote HTTPS: let the iframe/browser fetch normally. Never read a response.
        return null
    }

    private fun filterWorkerRequest(request: WebResourceRequest): WebResourceResponse? {
        if (released || securityFailureReported.get()) return deniedResponse()
        // Shared by the iframe and transported windows: a worker has no reliable WebView owner.
        // This is a network/resource gate, not permission to replace the iframe's main document.
        return if (
            IframeRequestPolicy.isAllowedRequest(request.url.toString(), isMainFrame = false)
        )
            null
        else deniedResponse()
    }

    private fun configureServiceWorkers() {
        if (
            WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) &&
                WebViewFeature.isFeatureSupported(
                    WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST
                )
        ) {
            val controller = ServiceWorkerControllerCompat.getInstance()
            val workerSettings = controller.serviceWorkerWebSettings
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_FILE_ACCESS))
                workerSettings.setAllowFileAccess(false)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_CONTENT_ACCESS))
                workerSettings.setAllowContentAccess(false)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_CACHE_MODE))
                workerSettings.setCacheMode(WebSettings.LOAD_NO_CACHE)
            if (
                WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS)
            )
                workerSettings.setBlockNetworkLoads(false)
            controller.setServiceWorkerClient(workerClient)
        } else {
            block(PlaybackBlock.local(PlaybackBlockKind.WEBVIEW_FEATURE))
        }
    }

    private fun block(diagnostic: PlaybackBlock) {
        if (!released && securityFailureReported.compareAndSet(false, true)) {
            post {
                if (!released) {
                    stopLoading()
                    onSecurityBlocked(diagnostic)
                }
            }
        }
    }

    fun release() {
        if (released) return
        released = true
        windows.closeOwner(windowOwner)
        fullscreen.hide()
        // After a renderer crash Android only permits removing and destroying this instance.
        if (!rendererGone) {
            stopLoading()
            onPause()
            clearHistory()
            clearCache(true)
            webChromeClient = null
            webViewClient = WebViewClient()
        }
        removeAllViews()
        destroy()
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        if (
            WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) &&
                WebViewFeature.isFeatureSupported(
                    WebViewFeature.SERVICE_WORKER_SHOULD_INTERCEPT_REQUEST
                )
        ) {
            val controller = ServiceWorkerControllerCompat.getInstance()
            // Do not retain a client that captures this Activity/WebView after destruction.
            controller.setServiceWorkerClient(
                object : ServiceWorkerClientCompat() {
                    override fun shouldInterceptRequest(request: WebResourceRequest) =
                        deniedResponse()
                }
            )
            if (
                WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BLOCK_NETWORK_LOADS)
            )
                controller.serviceWorkerWebSettings.setBlockNetworkLoads(true)
        }
    }

    companion object {
        private fun deniedResponse() =
            WebResourceResponse(
                "text/plain",
                "UTF-8",
                403,
                "Forbidden",
                mapOf("Cache-Control" to "no-store"),
                ByteArrayInputStream(ByteArray(0)),
            )
    }
}
