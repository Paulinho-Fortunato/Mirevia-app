package ao.mirevia.core.playback

import ao.mirevia.core.model.*

class ResolveVideoSource(
    private val provider: VideoSourceProvider,
    private val policy: EmbedUrlPolicy,
) {
    suspend operator fun invoke(
        details: MediaDetails,
        season: Int? = null,
        episode: Int? = null,
    ): VideoSource? {
        if (details.media.key.origin != DataOrigin.TMDB) return null
        val key = details.media.key
        val source =
            when (key.type) {
                MediaType.MOVIE -> {
                    if (season != null || episode != null) return null
                    provider.getMovieSource(
                        key.id,
                        details.imdbId,
                        details.media.title,
                        details.media.year,
                    )
                }
                MediaType.TV -> provider.getSeriesSource(key.id, season, episode)
            }
        return source?.takeIf(policy::isValidSource)
    }
}
