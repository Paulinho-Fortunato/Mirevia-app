package ao.mirevia.core.repository

import ao.mirevia.core.model.*
import kotlinx.coroutines.flow.Flow

interface CatalogRepository {
    val isDemo: Boolean

    suspend fun home(recommendationSeed: MediaKey? = null): HomeFeed

    suspend fun discover(
        type: MediaType,
        filters: DiscoverFilters,
        page: Int = 1,
    ): Page<MediaSummary>

    suspend fun search(query: String, scope: SearchScope, page: Int = 1): Page<SearchResult>

    suspend fun details(key: MediaKey): MediaDetails

    suspend fun season(key: MediaKey, number: Int): List<Episode>

    suspend fun person(id: Int): PersonDetails

    suspend fun genres(type: MediaType): List<Genre>
}

interface LibraryRepository {
    val entries: Flow<List<LibraryEntry>>
    val history: Flow<List<PlaybackEntry>>
    val searchHistory: Flow<List<String>>

    fun episodeStates(key: MediaKey): Flow<List<EpisodeWatchState>>

    suspend fun toggle(media: MediaSummary, flag: LibraryFlag)

    suspend fun remove(key: MediaKey)

    suspend fun markEpisode(key: MediaKey, season: Int, episode: Int, watched: Boolean)

    suspend fun recordOpened(media: MediaSummary, season: Int?, episode: Int?): Long

    suspend fun recordLeft(historyId: Long)

    suspend fun removeHistory(historyId: Long)

    suspend fun clearHistory()

    suspend fun rememberSearch(query: String)

    suspend fun clearSearchHistory()

    suspend fun clearAll()
}
