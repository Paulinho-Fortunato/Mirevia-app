package ao.mirevia.playback

import android.view.View
import android.webkit.WebChromeClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class EmbedFullscreenController {
    var customView: View? by mutableStateOf(null)
        private set

    private var callback: WebChromeClient.CustomViewCallback? = null

    fun show(view: View, nextCallback: WebChromeClient.CustomViewCallback) {
        if (customView != null) {
            nextCallback.onCustomViewHidden()
            return
        }
        callback = nextCallback
        customView = view
    }

    fun hide() {
        val old = callback
        callback = null
        customView = null
        old?.onCustomViewHidden()
    }
}
