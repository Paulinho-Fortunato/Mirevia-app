package ao.mirevia.data.remote.tmdb

import ao.mirevia.core.model.*
import java.net.URI

internal fun String?.available(): String? = this?.trim()?.takeIf(String::isNotEmpty)

/** Base and sizes are supplied by /configuration, never inferred from a poster identifier. */
class TmdbImages(private val configuration: ImageConfigurationDto) {
    private fun image(path: String?, sizes: List<String>, preferred: String): String? {
        if (path == null || !Regex("/[A-Za-z0-9_.-]+\\.(jpg|png|webp)").matches(path)) return null
        val base = runCatching { URI(configuration.secureBaseUrl) }.getOrNull() ?: return null
        if (
            base.scheme != "https" ||
                base.host != "image.tmdb.org" ||
                base.rawUserInfo != null ||
                base.rawQuery != null ||
                base.rawFragment != null ||
                base.port != -1 ||
                base.path != "/t/p/"
        )
            return null
        val size =
            preferred.takeIf { it in sizes }
                ?: sizes.firstOrNull { Regex("w[0-9]{2,4}").matches(it) }
                ?: return null
        return "${configuration.secureBaseUrl}$size$path"
    }

    fun poster(path: String?) = image(path, configuration.posterSizes, "w342")

    fun backdrop(path: String?) = image(path, configuration.backdropSizes, "w780")

    fun profile(path: String?) = image(path, configuration.profileSizes, "w185")

    fun still(path: String?) = image(path, configuration.stillSizes, "w300")
}

fun MediaDto.toSummary(images: TmdbImages, fallbackType: MediaType? = null): MediaSummary? {
    val type = MediaType.fromApi(mediaType) ?: fallbackType ?: return null
    if (id <= 0 || adult || mediaType == "person") return null
    return MediaSummary(
        key = MediaKey(DataOrigin.TMDB, type, id),
        title =
            (if (type == MediaType.MOVIE) title else name).available()
                ?: "Informação não disponível",
        originalTitle = (if (type == MediaType.MOVIE) originalTitle else originalName).available(),
        overview = overview.available(),
        posterUrl = images.poster(posterPath),
        backdropUrl = images.backdrop(backdropPath),
        releaseDate = (if (type == MediaType.MOVIE) releaseDate else firstAirDate).available(),
        rating = voteAverage?.takeIf { voteCount > 0 && it.isFinite() && it in 0.0..10.0 },
        genreIds = genreIds.filter { it > 0 },
        originalLanguage = originalLanguage.available(),
    )
}

fun MediaDto.toSearchResult(images: TmdbImages, scope: SearchScope): SearchResult? {
    if (id <= 0 || adult) return null
    if (mediaType == "person" || scope == SearchScope.PEOPLE) {
        return SearchResult.Person(
            PersonSummary(
                id,
                name.available() ?: "Informação não disponível",
                images.profile(profilePath),
                department,
            )
        )
    }
    val fallback =
        when (scope) {
            SearchScope.MOVIES -> MediaType.MOVIE
            SearchScope.SERIES -> MediaType.TV
            else -> null
        }
    return toSummary(images, fallback)?.let(SearchResult::Media)
}

fun CreditDto.toPerson(images: TmdbImages): PersonSummary? =
    if (id > 0) {
        PersonSummary(
            id,
            name.available() ?: "Informação não disponível",
            images.profile(profilePath),
            department,
            character.available() ?: job.available(),
        )
    } else null

fun MediaDetailsDto.toDetails(type: MediaType, images: TmdbImages): MediaDetails {
    if (id <= 0 || adult) throw ContentUnavailableException()
    val summary =
        MediaDto(
                id = id,
                title = title,
                name = name,
                originalTitle = originalTitle,
                originalName = originalName,
                overview = overview,
                posterPath = posterPath,
                backdropPath = backdropPath,
                releaseDate = releaseDate,
                firstAirDate = firstAirDate,
                voteAverage = voteAverage,
                voteCount = voteCount,
                genreIds = genres.map { it.id },
                originalLanguage = originalLanguage,
            )
            .toSummary(images, type) ?: throw ContentUnavailableException()
    return MediaDetails(
        media = summary,
        imdbId = imdbId.available() ?: externalIds?.imdbId.available(),
        genres =
            genres
                .filter { it.id > 0 }
                .map { Genre(it.id, it.name.ifBlank { "Informação não disponível" }) },
        runtimeMinutes = (runtime ?: episodeRunTime.firstOrNull())?.takeIf { it > 0 },
        countries =
            originCountry
                .filter { it.isNotBlank() }
                .ifEmpty { countries.mapNotNull { it.code.available() } },
        cast = credits?.cast.orEmpty().mapNotNull { it.toPerson(images) }.distinctBy { it.id },
        directors =
            credits
                ?.crew
                .orEmpty()
                .filter { it.job == "Director" }
                .mapNotNull { it.toPerson(images) }
                .distinctBy { it.id },
        creators = creators.mapNotNull { it.toPerson(images) },
        seasons =
            seasons
                .filter { it.id > 0 && it.number >= 0 }
                .map {
                    Season(
                        it.id,
                        it.number,
                        it.name.ifBlank { "Informação não disponível" },
                        it.overview.available(),
                        it.episodeCount,
                        it.airDate.available(),
                    )
                }
                .sortedBy { it.number },
        trailer = videos?.results.orEmpty().firstNotNullOfOrNull(VideoDto::toOfficialTrailer),
        recommendations =
            recommendations?.results.orEmpty().mapNotNull { it.toSummary(images, type) },
        similar = similar?.results.orEmpty().mapNotNull { it.toSummary(images, type) },
    )
}

fun VideoDto.toOfficialTrailer(): OfficialTrailer? {
    if (!official || type != "Trailer") return null
    val url =
        when {
            site == "YouTube" && Regex("[A-Za-z0-9_-]{11}").matches(key) ->
                "https://www.youtube.com/watch?v=$key"
            site == "Vimeo" && Regex("[0-9]{1,12}").matches(key) -> "https://vimeo.com/$key"
            else -> return null
        }
    return OfficialTrailer(name.ifBlank { "Trailer oficial" }, url)
}

fun EpisodeDto.toEpisode(images: TmdbImages): Episode? =
    if (id > 0 && season >= 0 && number > 0) {
        Episode(
            id,
            season,
            number,
            name.ifBlank { "Informação não disponível" },
            overview.available(),
            airDate.available(),
            runtime?.takeIf { it > 0 },
            images.still(stillPath),
        )
    } else null
