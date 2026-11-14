package ao.mirevia.core.playback

class EmbedMoviesProvider(allowedEmbedHosts: Set<String>) : VideoSourceProvider {
    private val policy = EmbedUrlPolicy(allowedEmbedHosts)
    private val urls = EmbedMoviesUrlBuilder(policy)

    override suspend fun getMovieSource(
        tmdbId: Int,
        imdbId: String?,
        title: String?,
        year: Int?,
    ): VideoSource? = urls.movie(tmdbId, imdbId)?.let(::source)

    override suspend fun getSeriesSource(tmdbId: Int, season: Int?, episode: Int?): VideoSource? =
        urls.series(tmdbId, season, episode)?.let(::source)

    private fun source(url: String) =
        VideoSource(
                url = url,
                type = VideoSourceType.IFRAME,
                providerName = "EmbedMovies",
                isIframe = true,
                requiresWebView = true,
                supportsFullscreen = true,
                // Website PiP claims do not establish an Android native PiP/event contract.
                supportsPictureInPicture = false,
            )
            .takeIf(policy::isValidSource)
}
