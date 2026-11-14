package ao.mirevia.core.playback

interface VideoSourceProvider {
    suspend fun getMovieSource(
        tmdbId: Int,
        imdbId: String?,
        title: String?,
        year: Int?,
    ): VideoSource?

    suspend fun getSeriesSource(
        tmdbId: Int,
        season: Int? = null,
        episode: Int? = null,
    ): VideoSource?
}

enum class VideoSourceType {
    IFRAME
}

data class VideoSource(
    val url: String,
    val type: VideoSourceType,
    val providerName: String,
    val isIframe: Boolean,
    val requiresWebView: Boolean,
    val supportsFullscreen: Boolean,
    val supportsPictureInPicture: Boolean,
    val subtitlesUrl: String? = null,
)
