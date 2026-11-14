package ao.mirevia.core.playback

import java.net.URI
import java.util.Locale

/**
 * Owner-selected browser-like HTTPS policy for the consented provider document, not for
 * VideoSource. Initial playback/share URLs still use the separate exact-host EmbedUrlPolicy. This
 * policy never fetches a response, resolves DNS or treats HTTPS as a safety certification.
 */
object IframeRequestPolicy {
    const val LOCAL_HOST = "appassets.androidplatform.net"
    const val LOCAL_DOCUMENT_URL = "https://$LOCAL_HOST/player/index.html"
    // Increment when third-party exposure changes; a legacy approval must not grant wider access.
    const val CONSENT_REVISION = 3
    // Host wildcard only for internal frames, with the same TLS/default-port boundary.
    const val FRAME_CSP_SOURCES = "https://*:443"
    const val RESOURCE_DESCRIPTION =
        "Recursos e frames internos: HTTPS (443), qualquer domínio remoto."

    fun hasCurrentConsent(accepted: Boolean, revision: Int?): Boolean =
        accepted && revision == CONSENT_REVISION

    fun isAllowedRequest(url: String, isMainFrame: Boolean): Boolean {
        if (isMainFrame) return url == LOCAL_DOCUMENT_URL
        if (
            url.isEmpty() ||
                url != url.trim() ||
                url.any { it <= ' ' || it == '\\' || it == '\u007f' }
        )
            return false
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase(Locale.ROOT) != "https" || uri.rawUserInfo != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        val host = uri.host?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() } ?: return false
        // Reserve this origin for the one locally served main document, never a remote request.
        if (host.trimEnd('.') == LOCAL_HOST) return false
        val authority = uri.rawAuthority?.lowercase(Locale.ROOT)
        return authority == host || authority == "$host:443"
    }
}
