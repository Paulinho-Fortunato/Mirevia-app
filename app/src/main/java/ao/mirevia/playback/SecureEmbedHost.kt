package ao.mirevia.playback

import android.content.Context
import android.widget.FrameLayout
import ao.mirevia.core.playback.EmbedUrlPolicy
import ao.mirevia.core.playback.PlaybackBlock
import ao.mirevia.core.playback.VideoSource

/**
 * Handles devices with an unavailable/disabled system WebView without crashing the Compose screen.
 */
class SecureEmbedHost(
    context: Context,
    source: VideoSource,
    policy: EmbedUrlPolicy,
    fullscreen: EmbedFullscreenController,
    onOpened: () -> Long,
    private val onClosed: (Long) -> Unit,
    onWrapperLoaded: () -> Unit,
    onFailure: () -> Unit,
    onBlocked: (PlaybackBlock) -> Unit,
) : FrameLayout(context) {
    private var embedded: SecureEmbedWebView? = null
    private var token: Long? = null

    init {
        try {
            val player =
                SecureEmbedWebView(
                    context,
                    source,
                    policy,
                    fullscreen,
                    0,
                    onWrapperLoaded,
                    onFailure,
                    onBlocked,
                )
            embedded = player
            addView(player, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
            token = onOpened()
        } catch (_: Exception) {
            embedded?.release()
            embedded = null
            onFailure()
        }
    }

    fun release() {
        embedded?.let {
            removeView(it)
            it.release()
        }
        embedded = null
        token?.let(onClosed)
        token = null
    }
}
