package ao.mirevia.data.repository

import ao.mirevia.core.model.*
import ao.mirevia.core.repository.CatalogRepository

/** Invalid non-empty configuration is an error, never a silent switch to invented results. */
class UnconfiguredCatalogRepository : CatalogRepository {
    override val isDemo = false

    override suspend fun home(recommendationSeed: MediaKey?): HomeFeed =
        throw ConfigurationException()

    override suspend fun discover(
        type: MediaType,
        filters: DiscoverFilters,
        page: Int,
    ): Page<MediaSummary> = throw ConfigurationException()

    override suspend fun search(query: String, scope: SearchScope, page: Int): Page<SearchResult> =
        throw ConfigurationException()

    override suspend fun details(key: MediaKey): MediaDetails = throw ConfigurationException()

    override suspend fun season(key: MediaKey, number: Int): List<Episode> =
        throw ConfigurationException()

    override suspend fun person(id: Int): PersonDetails = throw ConfigurationException()

    override suspend fun genres(type: MediaType): List<Genre> = throw ConfigurationException()
}
