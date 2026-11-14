package ao.mirevia.core.model

import ao.mirevia.core.playback.EmbedUrlPolicy
import ao.mirevia.core.playback.VideoSource
import java.net.URI

/** Public text only. No video files, bearer tokens, raw resource URLs or platform permissions. */
data class SharePayload(val subject: String, val text: String)

object MediaSharing {
    fun title(media: MediaSummary): SharePayload? {
        if (media.key.origin != DataOrigin.TMDB) return null
        val title = cleanTitle(media.title)
        return SharePayload(
            title,
            "$title\nhttps://www.themoviedb.org/${media.key.type.apiValue}/${media.key.id}",
        )
    }

    fun embed(media: MediaSummary, source: VideoSource?, policy: EmbedUrlPolicy): SharePayload? {
        if (media.key.origin != DataOrigin.TMDB || source == null || !policy.isValidSource(source))
            return null
        val parts = URI(source.url).path.split('/').drop(1)
        when (media.key.type) {
            MediaType.MOVIE -> {
                if (parts.first() != "filme") return null
                if (!parts[1].startsWith("tt") && parts[1] != media.key.id.toString()) return null
            }
            MediaType.TV -> {
                if (parts.first() != "serie" || parts[1] != media.key.id.toString()) return null
            }
        }
        val title = cleanTitle(media.title)
        val selection =
            if (parts.first() == "serie" && parts.size == 4)
                " · Temporada ${parts[2]} · Episódio ${parts[3]}"
            else ""
        return SharePayload(
            "$title$selection",
            "$title$selection\n${source.url}\n\nLink de incorporação MyEmbed. A reprodução funciona exclusivamente via iframe, não é um link directo de vídeo. A disponibilidade depende do fornecedor.",
        )
    }

    private fun cleanTitle(value: String): String =
        value
            .map { if (it.isISOControl()) ' ' else it }
            .joinToString("")
            .trim()
            .take(200)
            .ifBlank { "Título TMDB" }
}
