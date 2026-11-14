package ao.mirevia.data.repository

import ao.mirevia.core.model.*
import ao.mirevia.core.repository.CatalogRepository
import ao.mirevia.data.remote.tmdb.*
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TmdbCatalogRepository(
    private val api: TmdbApi,
    private val gate: ApiRequestGate = ApiRequestGate(),
) : CatalogRepository {
    override val isDemo = false
    private val configurationLock = Mutex()
    private var cachedImages: TmdbImages? = null
    private var configurationAt = 0L

    private suspend fun images(): TmdbImages =
        configurationLock.withLock {
            val now = System.currentTimeMillis()
            cachedImages?.takeIf { now - configurationAt < 86_400_000 }
                ?: TmdbImages(gate.execute { api.configuration() }.images).also {
                    cachedImages = it
                    configurationAt = now
                }
        }

    override suspend fun home(recommendationSeed: MediaKey?): HomeFeed = coroutineScope {
        val images = images()
        suspend fun row(type: MediaType, category: String, title: String): CatalogSection =
            CatalogSection(
                "${type.apiValue}_$category",
                title,
                gate
                    .execute { api.catalog(type.apiValue, category) }
                    .results
                    .mapNotNull { it.toSummary(images, type) },
            )
        val movies = async { row(MediaType.MOVIE, "popular", "Filmes populares") }
        val series = async { row(MediaType.TV, "popular", "Séries populares") }
        val trends = async {
            CatalogSection(
                "trending",
                "Tendências da semana",
                gate.execute { api.trending() }.results.mapNotNull { it.toSummary(images) },
            )
        }
        val top = async { row(MediaType.MOVIE, "top_rated", "Filmes mais bem avaliados") }
        val topTv = async { row(MediaType.TV, "top_rated", "Séries mais bem avaliadas") }
        val recent = async {
            val today = LocalDate.now()
            val filters =
                mapOf(
                    "include_adult" to "false",
                    "primary_release_date.gte" to today.minusMonths(3).toString(),
                    "primary_release_date.lte" to today.toString(),
                    "sort_by" to "popularity.desc",
                )
            CatalogSection(
                "recent",
                "Lançamentos recentes",
                gate
                    .execute { api.discover("movie", filters, 1) }
                    .results
                    .mapNotNull { it.toSummary(images, MediaType.MOVIE) },
            )
        }
        val recommendation = async {
            recommendationSeed
                ?.takeIf { it.origin == DataOrigin.TMDB }
                ?.let { seed ->
                    CatalogSection(
                        "recommendations",
                        "Porque adicionaste à tua biblioteca",
                        details(seed).recommendations,
                    )
                }
        }
        val rows =
            listOfNotNull(
                movies.await(),
                series.await(),
                trends.await(),
                top.await(),
                topTv.await(),
                recent.await(),
                recommendation.await(),
            )
        HomeFeed(trends.await().items.firstOrNull() ?: movies.await().items.firstOrNull(), rows)
    }

    override suspend fun discover(
        type: MediaType,
        filters: DiscoverFilters,
        page: Int,
    ): Page<MediaSummary> {
        require(page in 1..500)
        val images = images()
        val response = gate.execute { api.discover(type.apiValue, filters.toQuery(type), page) }
        return Page(
            response.results.mapNotNull { it.toSummary(images, type) },
            response.page,
            response.totalPages.coerceAtMost(500),
        )
    }

    override suspend fun search(query: String, scope: SearchScope, page: Int): Page<SearchResult> {
        require(page in 1..500)
        if (query.isBlank()) return Page(emptyList(), 1, 0)
        val images = images()
        val response = gate.execute { api.search(scope.apiValue, query.trim().take(200), page) }
        return Page(
            response.results.mapNotNull { it.toSearchResult(images, scope) },
            response.page,
            response.totalPages.coerceAtMost(500),
        )
    }

    override suspend fun details(key: MediaKey): MediaDetails {
        if (key.origin != DataOrigin.TMDB) throw ContentUnavailableException()
        val images = images()
        val response = gate.execute { api.details(key.type.apiValue, key.id) }
        if (response.id != key.id) throw ContentUnavailableException()
        return response.toDetails(key.type, images)
    }

    override suspend fun season(key: MediaKey, number: Int): List<Episode> {
        if (key.origin != DataOrigin.TMDB || key.type != MediaType.TV || number < 0)
            throw ContentUnavailableException()
        val images = images()
        return gate
            .execute { api.season(key.id, number) }
            .episodes
            .mapNotNull { it.toEpisode(images) }
            .filter { it.season == number }
    }

    override suspend fun person(id: Int): PersonDetails {
        if (id <= 0) throw ContentUnavailableException()
        val images = images()
        val dto = gate.execute { api.person(id) }
        if (dto.id != id) throw ContentUnavailableException()
        return PersonDetails(
            PersonSummary(
                dto.id,
                dto.name.available() ?: "Informação não disponível",
                images.profile(dto.profilePath),
                dto.department,
            ),
            dto.biography.available(),
            dto.birthday.available(),
            dto.birthplace.available(),
            (dto.credits?.cast.orEmpty() + dto.credits?.crew.orEmpty())
                .mapNotNull { it.toSummary(images) }
                .distinctBy { it.key },
        )
    }

    override suspend fun genres(type: MediaType) =
        gate
            .execute { api.genres(type.apiValue) }
            .genres
            .filter { it.id > 0 }
            .map { Genre(it.id, it.name) }
}
