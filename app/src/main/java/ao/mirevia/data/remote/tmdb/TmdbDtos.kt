package ao.mirevia.data.remote.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaPageDto(
    val page: Int = 1,
    val results: List<MediaDto> = emptyList(),
    @SerialName("total_pages") val totalPages: Int = 0,
)

@Serializable
data class MediaDto(
    val id: Int = 0,
    @SerialName("media_type") val mediaType: String? = null,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("known_for_department") val department: String? = null,
    val adult: Boolean = false,
)

@Serializable data class GenreDto(val id: Int = 0, val name: String = "")

@Serializable data class GenresDto(val genres: List<GenreDto> = emptyList())

@Serializable
data class CountryDto(@SerialName("iso_3166_1") val code: String = "", val name: String = "")

@Serializable
data class MediaDetailsDto(
    val id: Int = 0,
    val title: String? = null,
    val name: String? = null,
    @SerialName("original_title") val originalTitle: String? = null,
    @SerialName("original_name") val originalName: String? = null,
    val overview: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("original_language") val originalLanguage: String? = null,
    @SerialName("imdb_id") val imdbId: String? = null,
    val runtime: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    val genres: List<GenreDto> = emptyList(),
    @SerialName("origin_country") val originCountry: List<String> = emptyList(),
    @SerialName("production_countries") val countries: List<CountryDto> = emptyList(),
    val credits: CreditsDto? = null,
    @SerialName("created_by") val creators: List<CreditDto> = emptyList(),
    @SerialName("external_ids") val externalIds: ExternalIdsDto? = null,
    val videos: VideosDto? = null,
    val seasons: List<SeasonDto> = emptyList(),
    val recommendations: MediaPageDto? = null,
    val similar: MediaPageDto? = null,
    val adult: Boolean = false,
)

@Serializable data class ExternalIdsDto(@SerialName("imdb_id") val imdbId: String? = null)

@Serializable
data class CreditDto(
    val id: Int = 0,
    val name: String = "",
    val character: String? = null,
    val job: String? = null,
    val department: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
data class CreditsDto(
    val cast: List<CreditDto> = emptyList(),
    val crew: List<CreditDto> = emptyList(),
)

@Serializable data class VideosDto(val results: List<VideoDto> = emptyList())

@Serializable
data class VideoDto(
    val name: String = "",
    val key: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false,
)

@Serializable
data class SeasonDto(
    val id: Int = 0,
    @SerialName("season_number") val number: Int = -1,
    val name: String = "",
    val overview: String? = null,
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("air_date") val airDate: String? = null,
)

@Serializable data class SeasonDetailsDto(val episodes: List<EpisodeDto> = emptyList())

@Serializable
data class EpisodeDto(
    val id: Int = 0,
    @SerialName("season_number") val season: Int = -1,
    @SerialName("episode_number") val number: Int = 0,
    val name: String = "",
    val overview: String? = null,
    @SerialName("air_date") val airDate: String? = null,
    val runtime: Int? = null,
    @SerialName("still_path") val stillPath: String? = null,
)

@Serializable
data class ConfigurationDto(val images: ImageConfigurationDto = ImageConfigurationDto())

@Serializable
data class ImageConfigurationDto(
    @SerialName("secure_base_url") val secureBaseUrl: String = "",
    @SerialName("poster_sizes") val posterSizes: List<String> = emptyList(),
    @SerialName("backdrop_sizes") val backdropSizes: List<String> = emptyList(),
    @SerialName("profile_sizes") val profileSizes: List<String> = emptyList(),
    @SerialName("still_sizes") val stillSizes: List<String> = emptyList(),
)

@Serializable
data class PersonDetailsDto(
    val id: Int = 0,
    val name: String = "",
    val biography: String? = null,
    val birthday: String? = null,
    @SerialName("place_of_birth") val birthplace: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("known_for_department") val department: String? = null,
    @SerialName("combined_credits") val credits: CombinedCreditsDto? = null,
)

@Serializable
data class CombinedCreditsDto(
    val cast: List<MediaDto> = emptyList(),
    val crew: List<MediaDto> = emptyList(),
)
