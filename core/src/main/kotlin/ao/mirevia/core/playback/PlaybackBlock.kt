package ao.mirevia.core.playback

import java.net.URI
import java.util.Locale

enum class PlaybackBlockKind {
    RESOURCE,
    NAVIGATION,
    NEW_WINDOW,
    DOWNLOAD,
    UNSAFE_PAGE,
    WEBVIEW_FEATURE,
}

/**
 * In-memory diagnostic, deliberately unable to retain a request URL, path, credentials or query.
 * Does not change the initial-source allowlist or fetch/inspect responses. No DNS/network lookup.
 */
class PlaybackBlock
private constructor(
    val kind: PlaybackBlockKind,
    val host: String?,
    val scheme: String?,
    val port: Int?,
) {
    fun report(policy: EmbedUrlPolicy): String =
        listOf(
                "Mirevia — diagnóstico local do iframe",
                "Motivo: ${kind.name}",
                "Domínio: ${host ?: "não identificado"}",
                "Esquema: ${scheme ?: "não identificado"}",
                "Porta explícita: ${port?.toString() ?: "nenhuma"}",
                "Iframe inicial autorizado: ${policy.allowedHosts.sorted().joinToString(", ")}",
                IframeRequestPolicy.RESOURCE_DESCRIPTION,
                "O domínio observado não é adicionado à lista de fontes iniciais.",
                "Sem caminhos, parâmetros, cookies, tokens ou histórico de títulos.",
            )
            .joinToString("\n")

    companion object {
        private val SAFE_SCHEME = Regex("[a-z][a-z0-9+.-]{0,31}")
        private val SAFE_HOST = Regex("[a-z0-9](?:[a-z0-9.-]{0,251}[a-z0-9.])?")
        private val IPV6_HOST = Regex("\\[[0-9a-f:]{2,45}]")

        fun local(kind: PlaybackBlockKind) = PlaybackBlock(kind, null, null, null)

        fun request(kind: PlaybackBlockKind, url: String?): PlaybackBlock {
            if (
                url == null ||
                    url.length > 8192 ||
                    url.any { it <= ' ' || it == '\\' || it == '\u007f' }
            )
                return local(kind)
            val uri = runCatching { URI(url) }.getOrNull() ?: return local(kind)
            val host =
                uri.host?.lowercase(Locale.ROOT)?.takeIf {
                    SAFE_HOST.matches(it) || IPV6_HOST.matches(it)
                }
            val scheme = uri.scheme?.lowercase(Locale.ROOT)?.takeIf(SAFE_SCHEME::matches)
            return PlaybackBlock(kind, host, scheme, uri.port.takeIf { it in 1..65535 })
        }
    }
}
