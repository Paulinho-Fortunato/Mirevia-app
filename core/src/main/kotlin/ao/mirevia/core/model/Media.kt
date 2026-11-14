package ao.mirevia.core.model

enum class DataOrigin {
    TMDB,
    DEMO,
}

enum class MediaType(val apiValue: String, val label: String) {
    MOVIE("movie", "Filme"),
    TV("tv", "Série");

    companion object {
        fun fromApi(value: String?) = entries.find { it.apiValue == value }
    }
}

data class MediaKey(val origin: DataOrigin, val type: MediaType, val id: Int) {
    init {
        require(id > 0) { "Invalid media identifier" }
    }

    val storageKey: String
        get() = "${origin.name}:${type.name}:$id"
}

data class Genre(val id: Int, val name: String)

data class MediaSummary(
    val key: MediaKey,
    val title: String,
    val originalTitle: String? = null,
    val overview: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val releaseDate: String? = null,
    val rating: Double? = null,
    val genreIds: List<Int> = emptyList(),
    val originalLanguage: String? = null,
) {
    val year: Int?
        get() = releaseDate?.take(4)?.toIntOrNull()

    val isDemo: Boolean
        get() = key.origin == DataOrigin.DEMO
}

data class PersonSummary(
    val id: Int,
    val name: String,
    val profileUrl: String? = null,
    val department: String? = null,
    val role: String? = null,
)

data class PersonDetails(
    val person: PersonSummary,
    val biography: String? = null,
    val birthday: String? = null,
    val birthplace: String? = null,
    val filmography: List<MediaSummary> = emptyList(),
)

data class Season(
    val id: Int,
    val number: Int,
    val title: String,
    val overview: String? = null,
    val episodeCount: Int? = null,
    val airDate: String? = null,
)

data class Episode(
    val id: Int,
    val season: Int,
    val number: Int,
    val title: String,
    val overview: String? = null,
    val airDate: String? = null,
    val runtimeMinutes: Int? = null,
    val stillUrl: String? = null,
)

data class OfficialTrailer(val name: String, val url: String)

data class MediaDetails(
    val media: MediaSummary,
    val imdbId: String? = null,
    val genres: List<Genre> = emptyList(),
    val runtimeMinutes: Int? = null,
    val countries: List<String> = emptyList(),
    val cast: List<PersonSummary> = emptyList(),
    val directors: List<PersonSummary> = emptyList(),
    val creators: List<PersonSummary> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val trailer: OfficialTrailer? = null,
    val recommendations: List<MediaSummary> = emptyList(),
    val similar: List<MediaSummary> = emptyList(),
)

data class Page<T>(val items: List<T>, val page: Int, val totalPages: Int) {
    val hasMore: Boolean
        get() = page < totalPages.coerceAtMost(500)
}

data class CatalogSection(val id: String, val title: String, val items: List<MediaSummary>)

data class HomeFeed(val featured: MediaSummary?, val sections: List<CatalogSection>)

enum class SearchScope(val apiValue: String, val label: String) {
    ALL("multi", "Tudo"),
    MOVIES("movie", "Filmes"),
    SERIES("tv", "Séries"),
    PEOPLE("person", "Pessoas"),
}

sealed interface SearchResult {
    val stableKey: String

    data class Media(val media: MediaSummary) : SearchResult {
        override val stableKey
            get() = media.key.storageKey
    }

    data class Person(val person: PersonSummary) : SearchResult {
        override val stableKey
            get() = "person:${person.id}"
    }
}

data class DiscoverFilters(
    val genreId: Int? = null,
    val year: Int? = null,
    val minimumRating: Int? = null,
    val originalLanguage: String? = null,
) {
    init {
        require(genreId == null || genreId > 0)
        require(year == null || year in 1870..2200)
        require(minimumRating == null || minimumRating in 0..10)
        require(originalLanguage == null || Regex("[a-z]{2}").matches(originalLanguage))
    }

    fun toQuery(type: MediaType): Map<String, String> = buildMap {
        put("include_adult", "false")
        put("sort_by", "popularity.desc")
        genreId?.let { put("with_genres", it.toString()) }
        year?.let {
            put(
                if (type == MediaType.MOVIE) "primary_release_year" else "first_air_date_year",
                it.toString(),
            )
        }
        minimumRating?.let {
            put("vote_average.gte", it.toString())
            put("vote_count.gte", "50")
        }
        originalLanguage?.let { put("with_original_language", it) }
    }
}

class ContentUnavailableException : Exception("Information unavailable")

class ConfigurationException : Exception("Invalid API configuration")
