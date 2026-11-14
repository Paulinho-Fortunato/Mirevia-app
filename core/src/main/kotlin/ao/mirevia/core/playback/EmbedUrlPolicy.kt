package ao.mirevia.core.playback

import java.net.URI
import java.util.Locale

/**
 * Initial-source/share policy: exact hosts only, without suffix matching or implicit subdomain
 * trust. Consented iframe resource/frame requests use IframeRequestPolicy instead.
 */
class EmbedUrlPolicy(allowedHosts: Set<String>) {
    val allowedHosts: Set<String> = allowedHosts.map { it.lowercase(Locale.ROOT) }.toSet()

    init {
        require(this.allowedHosts.isNotEmpty())
        require(this.allowedHosts.all { HOST.matches(it) && !it.endsWith('.') })
    }

    fun isAllowedNavigation(url: String): Boolean {
        val uri = parse(url) ?: return false
        if (uri.scheme != "https" || uri.rawUserInfo != null) return false
        if (uri.port != -1 && uri.port != 443) return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        if (host !in allowedHosts) return false
        // Backslashes, control characters and encoded authority tricks are rejected before URI
        // parsing.
        return uri.rawAuthority?.lowercase(Locale.ROOT) == host ||
            uri.rawAuthority?.lowercase(Locale.ROOT) == "$host:443"
    }

    fun isValidSource(source: VideoSource): Boolean =
        source.type == VideoSourceType.IFRAME &&
            source.isIframe &&
            source.requiresWebView &&
            source.subtitlesUrl == null &&
            isOfficialEmbedUrl(source.url)

    fun isOfficialEmbedUrl(url: String): Boolean {
        if (!isAllowedNavigation(url)) return false
        val uri = parse(url) ?: return false
        if (uri.rawQuery != null || uri.rawFragment != null) return false
        val path = uri.rawPath.orEmpty()
        if (!MOVIE_PATH.matches(path) && !SERIES_PATH.matches(path)) return false
        val identifiers = path.split('/').drop(2)
        return identifiers.all { id ->
            if (id.startsWith("tt")) id.drop(2).any { it != '0' }
            else id.toIntOrNull()?.let { it > 0 } == true
        }
    }

    private fun parse(url: String): URI? {
        if (
            url.isEmpty() ||
                url != url.trim() ||
                url.any { it <= ' ' || it == '\\' || it == '\u007f' }
        )
            return null
        return runCatching { URI(url) }.getOrNull()
    }

    companion object {
        private val HOST =
            Regex(
                "(?=.{1,253}$)[a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+"
            )
        private val ID = "[1-9][0-9]{0,9}"
        private val MOVIE_PATH = Regex("/filme/(?:$ID|tt[0-9]{7,10})")
        private val SERIES_PATH = Regex("/serie/$ID(?:/$ID/$ID)?")
    }
}
