package ao.mirevia.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.webkit.WebView
import androidx.compose.runtime.*
import ao.mirevia.core.playback.PlaybackBlock
import ao.mirevia.core.playback.PlayerWindowStack

/** Native handles stay in the UI layer. No URL, response, cookies or history in this state. */
class PlayerWindowPage internal constructor() {
    internal lateinit var browser: PlayerWindowWebView
    var origin: PlaybackBlock? by mutableStateOf(null)
        internal set

    var loading by mutableStateOf(false)
        internal set

    var failure: String? by mutableStateOf(null)
        internal set

    internal fun releaseIfCreated() {
        if (this::browser.isInitialized) browser.release()
    }
}

/** Owns transported windows, not the iframe. Call from the main thread, including on release. */
class PlayerWindowController(private val onNotice: (String) -> Unit) {
    private val stack = PlayerWindowStack<PlayerWindowPage>()
    private val handler = Handler(Looper.getMainLooper())
    private val pending = mutableMapOf<Long, Pending>()
    private var focusedId: Long? by mutableStateOf(null)
    private var entries by mutableStateOf(emptyList<PlayerWindowStack.Entry<PlayerWindowPage>>())
    val activeWindow
        get() = entries.firstOrNull { it.id == focusedId } ?: entries.lastOrNull()

    private class Pending(
        val message: Message,
        val transport: WebView.WebViewTransport,
        val listener: View.OnAttachStateChangeListener,
        val timeout: Runnable,
    )

    internal fun open(context: Context, owner: Any, openerId: Long?, message: Message): Boolean {
        val transport = message.obj as? WebView.WebViewTransport
        if (transport == null || message.target == null) {
            onNotice(
                "Não foi possível abrir a janela do player. O iframe principal não foi encerrado."
            )
            return false
        }
        // Chromium keeps one pending popup per opener. Do not overwrite that native request
        // while Compose is attaching its view; reject only the extra window, never the iframe.
        if (
            stack.entries.any { it.owner === owner && it.openerId == openerId && it.id in pending }
        ) {
            onNotice("Aguarda a abertura da janela do player antes de pedir outra.")
            return false
        }
        val page = PlayerWindowPage()
        val entry = stack.add(owner, openerId, page)
        if (entry == null) {
            onNotice(
                "Fecha uma janela do player antes de abrir outra. Podem estar abertas até três janelas."
            )
            return false
        }
        try {
            page.browser =
                PlayerWindowWebView(
                    context,
                    onOpen = { result -> open(context, owner, entry.id, result) },
                    onClose = { window -> closeView(owner, window) },
                    onFocus = { focus(entry.id) },
                    onOrigin = { origin -> page.origin = origin },
                    onLoading = { loading -> page.loading = loading },
                    onFailure = { text, origin -> fail(entry.id, text, origin) },
                )
            val listener =
                object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(view: View) {
                        completeTransport(entry.id)
                    }

                    override fun onViewDetachedFromWindow(view: View) = Unit
                }
            val timeout = Runnable {
                if (pending.containsKey(entry.id)) {
                    close(entry.id)
                    onNotice(
                        "Não foi possível apresentar a janela do player. O iframe principal mantém-se aberto."
                    )
                }
            }
            pending[entry.id] = Pending(message, transport, listener, timeout)
            page.browser.addOnAttachStateChangeListener(listener)
            handler.postDelayed(timeout, TRANSPORT_TIMEOUT_MS)
            focusedId = entry.id
            publish()
            // Compose attaches the new WebView before sending the native transport response.
            // No loadUrl, guessed destination, temporary wrapper, or provider request here.
            return true
        } catch (_: Exception) {
            discard(entry.id, sendCancellation = false)
            onNotice(
                "Não foi possível criar a janela do player. O iframe principal não foi encerrado."
            )
            return false
        }
    }

    private fun completeTransport(id: Long) {
        val entry = stack.entries.firstOrNull { it.id == id } ?: return
        if (!entry.value.browser.isAttachedToWindow) return
        val response = pending.remove(id) ?: return
        detachPending(entry.value, response)
        try {
            response.transport.webView = entry.value.browser
            response.message.sendToTarget()
            entry.value.browser.requestFocus()
        } catch (_: RuntimeException) {
            fail(
                id,
                "Não foi possível ligar a janela ao player. Podes fechá-la e tentar novamente.",
                null,
            )
        }
    }

    internal fun closeView(owner: Any, view: WebView) {
        stack.entries
            .firstOrNull { it.owner === owner && it.value.browser === view }
            ?.let { close(it.id) }
    }

    private fun focus(id: Long) {
        if (stack.entries.any { it.id == id }) focusedId = id
    }

    private fun fail(id: Long, text: String, origin: PlaybackBlock?) {
        val entry = stack.entries.firstOrNull { it.id == id } ?: return
        entry.value.origin = origin ?: entry.value.origin
        entry.value.loading = false
        entry.value.failure = text
        cancelPending(entry)
        // Only this failed window is destroyed, not the iframe, cookies or another session.
        entry.value.browser.release()
    }

    fun closeActive(): Boolean {
        val id = activeWindow?.id ?: return false
        close(id)
        return true
    }

    fun close(id: Long) {
        discard(id, sendCancellation = true)
    }

    private fun discard(id: Long, sendCancellation: Boolean) {
        release(stack.remove(id), sendCancellation)
    }

    internal fun closeOwner(owner: Any) {
        release(stack.removeOwner(owner), true)
    }

    fun closeAll() {
        release(stack.clear(), true)
    }

    private fun release(
        removed: List<PlayerWindowStack.Entry<PlayerWindowPage>>,
        sendCancellation: Boolean,
    ) {
        for (entry in removed) {
            cancelPending(entry, sendCancellation)
            // Construction can fail before assigning a browser.
            entry.value.releaseIfCreated()
        }
        publish()
        if (entries.none { it.id == focusedId }) focusedId = entries.lastOrNull()?.id
    }

    private fun cancelPending(
        entry: PlayerWindowStack.Entry<PlayerWindowPage>,
        sendCancellation: Boolean = true,
    ) {
        val response = pending.remove(entry.id) ?: return
        detachPending(entry.value, response)
        if (sendCancellation)
            runCatching {
                response.transport.webView = null
                response.message.sendToTarget()
            }
    }

    private fun detachPending(page: PlayerWindowPage, response: Pending) {
        page.browser.removeOnAttachStateChangeListener(response.listener)
        handler.removeCallbacks(response.timeout)
    }

    private fun publish() {
        entries = stack.entries
    }

    companion object {
        private const val TRANSPORT_TIMEOUT_MS = 15_000L
    }
}
