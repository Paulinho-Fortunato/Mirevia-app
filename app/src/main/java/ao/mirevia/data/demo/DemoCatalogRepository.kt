package ao.mirevia.data.demo

import ao.mirevia.core.model.*
import ao.mirevia.core.repository.CatalogRepository

/** All titles below are fictional UI fixtures. IDs are local, NOT TMDB identifiers. */
class DemoCatalogRepository : CatalogRepository {
    override val isDemo = true
    private val fixtures =
        listOf(
                "Além do horizonte",
                "O último sinal",
                "Maré de vidro",
                "Entre estrelas",
                "Cidade suspensa",
                "A ilha do tempo",
                "Linhas do amanhã",
                "O lado da luz",
            )
            .mapIndexed { index, title ->
                MediaSummary(
                    key =
                        MediaKey(
                            DataOrigin.DEMO,
                            if (index % 3 == 1) MediaType.TV else MediaType.MOVIE,
                            index + 1,
                        ),
                    title = title,
                    overview =
                        "Exemplo fictício criado para demonstrar a interface da Mirevia. Não representa uma obra real. Configura a TMDB para descobrir filmes, séries e pessoas reais.",
                )
            }

    override suspend fun home(recommendationSeed: MediaKey?) =
        HomeFeed(
            fixtures.first(),
            listOf(
                CatalogSection(
                    "demo_movies",
                    "Filmes · exemplos de interface",
                    fixtures.filter { it.key.type == MediaType.MOVIE },
                ),
                CatalogSection(
                    "demo_tv",
                    "Séries · exemplos de interface",
                    fixtures.filter { it.key.type == MediaType.TV },
                ),
                CatalogSection(
                    "demo_design",
                    "Descobre a tua próxima história · demonstração",
                    fixtures.reversed(),
                ),
            ),
        )

    override suspend fun discover(
        type: MediaType,
        filters: DiscoverFilters,
        page: Int,
    ): Page<MediaSummary> {
        val hasRealFilters =
            filters.genreId != null ||
                filters.year != null ||
                filters.minimumRating != null ||
                filters.originalLanguage != null
        return Page(
            if (page == 1 && !hasRealFilters) fixtures.filter { it.key.type == type }
            else emptyList(),
            page,
            1,
        )
    }

    override suspend fun search(query: String, scope: SearchScope, page: Int): Page<SearchResult> =
        Page(
            if (page != 1 || scope == SearchScope.PEOPLE) emptyList()
            else
                fixtures
                    .filter {
                        it.title.contains(query, ignoreCase = true) &&
                            when (scope) {
                                SearchScope.MOVIES -> it.key.type == MediaType.MOVIE
                                SearchScope.SERIES -> it.key.type == MediaType.TV
                                else -> true
                            }
                    }
                    .map(SearchResult::Media),
            page,
            1,
        )

    override suspend fun details(key: MediaKey): MediaDetails =
        MediaDetails(
            fixtures.find { it.key == key } ?: throw ContentUnavailableException(),
            similar = fixtures.filter { it.key != key }.take(5),
        )

    override suspend fun season(key: MediaKey, number: Int): List<Episode> = emptyList()

    override suspend fun person(id: Int): PersonDetails = throw ContentUnavailableException()

    override suspend fun genres(type: MediaType): List<Genre> = emptyList()
}
