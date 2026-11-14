package ao.mirevia.core.playback

/** Routes verified against https://embedmovies.org/#guia-rapido on 2026-11-14. */
class EmbedMoviesUrlBuilder(private val policy: EmbedUrlPolicy) {
    fun movie(tmdbId: Int, imdbId: String?): String? {
        if (tmdbId <= 0 || (imdbId != null && !IMDB_ID.matches(imdbId))) return null
        val identifier = imdbId ?: tmdbId.toString()
        return validate("https://$DOCUMENTED_HOST/filme/$identifier")
    }

    fun series(tmdbId: Int, season: Int?, episode: Int?): String? {
        if (tmdbId <= 0) return null
        if ((season == null) != (episode == null)) return null
        if (season != null && (season <= 0 || episode == null || episode <= 0)) return null
        val suffix = if (season != null && episode != null) "/$season/$episode" else ""
        return validate("https://$DOCUMENTED_HOST/serie/$tmdbId$suffix")
    }

    private fun validate(url: String): String? = url.takeIf(policy::isOfficialEmbedUrl)

    companion object {
        const val DOCUMENTED_HOST = "myembed.biz"
        private val IMDB_ID = Regex("tt[0-9]{7,10}")
    }
}
