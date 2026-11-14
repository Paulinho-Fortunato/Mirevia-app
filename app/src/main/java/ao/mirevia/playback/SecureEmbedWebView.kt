package ao.mirevia.playback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.view.View
import android.webkit.*
import androidx.webkit.*
import ao.mirevia.core.playback.EmbedUrlPolicy
import ao.mirevia.core.playback.VideoSource
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Suppress("DEPRECATION")
class SecureEmbedWebView(
    context: Context,
    private val source: VideoSource,
    private val policy: EmbedUrlPolicy,
    private val fullscreen: EmbedFullscreenController,
    val sessionToken: Long,
    private val onWrapperLoaded: () -> Unit,
    private val onFailure: () -> Unit,
    private val onSecurityBlocked: (String) -> Unit,
    autoLoad: Boolean = true,
) : WebView(context) {
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
        setBackgroundColor(android.graphics.Color.BLACK)
        WebView.setWebContentsDebuggingEnabled(false)
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled =
                true // Needed by provider; cleanup is requested when the session ends.
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
            setAcceptThirdPartyCookies(this@SecureEmbedWebView, false)
        }
        configureServiceWorkers()
        setDownloadListener { _, _, _, _, _ ->
            block("A Mirevia não permite downloads nem a abertura de ficheiros de vídeo.")
        }
        webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val url = request.url.toString()
                    val allowed =
                        if (request.isForMainFrame) url == EmbedPage.URL
                        else policy.isAllowedNavigation(url)
                    if (!allowed)
                        block(
                            "Foi bloqueada uma navegação não autorizada. O conteúdo deve permanecer no iframe aprovado."
                        )
                    return !allowed
                }

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = filterRequest(request)

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    if (url != EmbedPage.URL) {
                        stopLoading()
                        block("O player tentou sair da página de incorporação autorizada.")
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
                    block("O Android identificou uma navegação insegura e bloqueou-a.")
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
        webChromeClient =
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
                    block(
                        "Foi bloqueada uma janela externa que não pode ser validada em segurança."
                    )
                    return false
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
        if (autoLoad && !securityFailureReported.get()) loadUrl(EmbedPage.URL)
    }

    private fun filterRequest(request: WebResourceRequest): WebResourceResponse? {
        if (released) return deniedResponse()
        val url = request.url.toString()
        if (url == EmbedPage.URL)
            return assetLoader.shouldInterceptRequest(request.url) ?: deniedResponse()
        if (policy.isAllowedNavigation(url))
            return null // Let WebView fetch normally; never read or extract a response.
        block(
            "O player pediu um recurso de um domínio ou esquema não autorizado. A sessão foi interrompida por segurança."
        )
        return deniedResponse()
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
            controller.setServiceWorkerClient(
                object : ServiceWorkerClientCompat() {
                    override fun shouldInterceptRequest(
                        request: WebResourceRequest
                    ): WebResourceResponse? = filterRequest(request)
                }
            )
        } else {
            block(
                "Actualiza o Android System WebView. Esta versão não permite aplicar a política de segurança necessária aos service workers."
            )
        }
    }

    private fun block(message: String) {
        if (!released && securityFailureReported.compareAndSet(false, true)) {
            post { if (!released) onSecurityBlocked(message) }
        }
    }

    fun release() {
        if (released) return
        released = true
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
