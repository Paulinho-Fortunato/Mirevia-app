package ao.mirevia.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import ao.mirevia.core.playback.PlaybackBlock
import ao.mirevia.core.playback.PlaybackBlockKind
import ao.mirevia.core.playback.PlayerWindowPolicy
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean

/** An actual engine-created window. No app-selected URL, iframe replacement or external intents. */
internal class PlayerWindowWebView(
    context: Context,
    private val onOpen: (Message) -> Boolean,
    private val onClose: (WebView) -> Unit,
    private val onFocus: () -> Unit,
    private val onOrigin: (PlaybackBlock) -> Unit,
    private val onLoading: (Boolean) -> Unit,
    private val onFailure: (String, PlaybackBlock?) -> Unit,
) : WebView(context) {
    @Volatile private var released = false
    private var rendererGone = false
    private val failed = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    internal val requestClient: WebViewClient
    internal val chromeClient: WebChromeClient

    init {
        configurePlayerBrowser()
        setDownloadListener { _, _, _, _, _ ->
            fail(
                "Downloads não estão disponíveis nesta janela.",
                PlaybackBlock.local(PlaybackBlockKind.DOWNLOAD),
            )
        }
        requestClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    if (released || failed.get()) return true
                    val url = request.url.toString()
                    val allowed = PlayerWindowPolicy.isAllowedRequest(url)
                    if (!allowed) denied(url, PlaybackBlockKind.NAVIGATION)
                    return !allowed
                }

                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? {
                    if (released || failed.get()) return deniedResponse()
                    val url = request.url.toString()
                    if (PlayerWindowPolicy.isAllowedRequest(url)) return null
                    denied(url, PlaybackBlockKind.RESOURCE)
                    return deniedResponse()
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    if (released || failed.get()) return
                    if (updateDocumentOrigin(url)) onLoading(true)
                }

                override fun onPageCommitVisible(view: WebView, url: String?) {
                    if (!released && !failed.get()) updateDocumentOrigin(url)
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    if (!released && !failed.get() && updateDocumentOrigin(url))
                        onLoading(false) // Page only, never video progress.
                }

                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError,
                ) {
                    if (request.isForMainFrame) fail("Não foi possível carregar esta janela.", null)
                }

                override fun onReceivedHttpError(
                    view: WebView,
                    request: WebResourceRequest,
                    errorResponse: WebResourceResponse,
                ) {
                    if (request.isForMainFrame && errorResponse.statusCode >= 400)
                        fail(
                            "O serviço desta janela devolveu um erro. Podes fechar a janela e regressar ao player.",
                            null,
                        )
                }

                override fun onReceivedSslError(
                    view: WebView,
                    handler: SslErrorHandler,
                    error: SslError,
                ) {
                    handler.cancel()
                    fail(
                        "A ligação desta janela falhou a validação TLS. Não foi aberta uma excepção de segurança.",
                        null,
                    )
                }

                @androidx.annotation.RequiresApi(27)
                override fun onSafeBrowsingHit(
                    view: WebView,
                    request: WebResourceRequest,
                    threatType: Int,
                    callback: SafeBrowsingResponse,
                ) {
                    callback.backToSafety(true)
                    fail(
                        "O Safe Browsing interrompeu esta janela. A protecção mantém-se activa.",
                        PlaybackBlock.request(PlaybackBlockKind.UNSAFE_PAGE, request.url.toString()),
                    )
                }

                @androidx.annotation.RequiresApi(26)
                override fun onRenderProcessGone(
                    view: WebView,
                    detail: RenderProcessGoneDetail,
                ): Boolean {
                    rendererGone = true
                    fail("O processo WebView desta janela foi interrompido.", null)
                    return true
                }
            }
        chromeClient =
            object : WebChromeClient() {
                override fun onCreateWindow(
                    view: WebView,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: Message,
                ): Boolean = !released && !failed.get() && onOpen(resultMsg)

                override fun onCloseWindow(window: WebView) {
                    if (!released) onClose(window)
                }

                override fun onRequestFocus(view: WebView) {
                    if (!released && !failed.get()) onFocus()
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
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams,
                ): Boolean {
                    filePathCallback.onReceiveValue(null)
                    return true
                }

                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    callback.onCustomViewHidden() // No new native fullscreen/PiP capability for
                    // windows.
                }

                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean = true
            }
        webViewClient = requestClient
        webChromeClient = chromeClient
        // Intentionally no loadUrl: WebViewTransport must receive a fresh, unnavigated WebView.
        // Service-worker configuration/cleanup belongs to the owning iframe, never a child window.
    }

    private fun updateDocumentOrigin(url: String?): Boolean {
        if (url != null && !PlayerWindowPolicy.isAllowedRequest(url)) {
            denied(url, PlaybackBlockKind.NAVIGATION)
            return false
        }
        // Refresh on commit/finish as well as start: a redirected window must not keep displaying
        // its original host. Discard paths/queries immediately, including on error or unknown URL.
        onOrigin(PlaybackBlock.request(PlaybackBlockKind.NAVIGATION, url))
        return true
    }

    private fun denied(url: String, kind: PlaybackBlockKind) {
        fail(
            "Esta janela só permite HTTPS na porta 443, sem credenciais no endereço, ficheiros ou esquemas externos. Não existe uma lista de domínios permitidos.",
            PlaybackBlock.request(kind, url),
        )
    }

    private fun fail(text: String, origin: PlaybackBlock?) {
        if (!released && failed.compareAndSet(false, true))
            mainHandler.post {
                if (!released) {
                    if (!rendererGone) stopLoading()
                    onFailure(text, origin)
                }
            }
    }

    internal fun release() {
        if (released) return
        released = true
        mainHandler.removeCallbacksAndMessages(null)
        (parent as? ViewGroup)?.removeView(this)
        if (!rendererGone) {
            stopLoading()
            onPause()
            clearHistory()
            webChromeClient = null
            webViewClient = WebViewClient()
        }
        removeAllViews()
        destroy()
        // Do not clear global cookies/storage or replace the shared worker client while the iframe
        // lives.
    }

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
