package ao.mirevia.ui

import ao.mirevia.core.model.*
import ao.mirevia.core.repository.*
import ao.mirevia.data.demo.DemoCatalogRepository
import ao.mirevia.ui.search.SearchViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private class TestLibrary : LibraryRepository {
        override val entries = flowOf(emptyList<LibraryEntry>())
        override val history = flowOf(emptyList<PlaybackEntry>())
        override val searchHistory = flowOf(emptyList<String>())
        val remembered = mutableListOf<String>()

        override fun episodeStates(key: MediaKey) = flowOf(emptyList<EpisodeWatchState>())

        override suspend fun toggle(media: MediaSummary, flag: LibraryFlag) = Unit

        override suspend fun remove(key: MediaKey) = Unit

        override suspend fun markEpisode(
            key: MediaKey,
            season: Int,
            episode: Int,
            watched: Boolean,
        ) = Unit

        override suspend fun recordOpened(media: MediaSummary, season: Int?, episode: Int?) = 1L

        override suspend fun recordLeft(historyId: Long) = Unit

        override suspend fun removeHistory(historyId: Long) = Unit

        override suspend fun clearHistory() = Unit

        override suspend fun rememberSearch(query: String) {
            remembered += query
        }

        override suspend fun clearSearchHistory() = Unit

        override suspend fun clearAll() = Unit
    }

    @Test
    fun `debounces requests and stores only the executed query`() =
        runTest(main.dispatcher) {
            val calls = mutableListOf<String>()
            val catalog =
                object : CatalogRepository by DemoCatalogRepository() {
                    override suspend fun search(
                        query: String,
                        scope: SearchScope,
                        page: Int,
                    ): Page<SearchResult> {
                        calls += query
                        return Page(emptyList(), 1, 0)
                    }
                }
            val library = TestLibrary()
            val vm = SearchViewModel(catalog, library)
            vm.setQuery("hori")
            advanceTimeBy(200)
            vm.setQuery("horizonte")
            advanceTimeBy(349)
            runCurrent()
            assertTrue(calls.isEmpty())
            advanceTimeBy(1)
            runCurrent()
            assertEquals(listOf("horizonte"), calls)
            assertEquals(listOf("horizonte"), library.remembered)
        }

    @Test
    fun `cancels stale queries without replacing the latest results`() =
        runTest(main.dispatcher) {
            val completed = mutableListOf<String>()
            val catalog =
                object : CatalogRepository by DemoCatalogRepository() {
                    override suspend fun search(
                        query: String,
                        scope: SearchScope,
                        page: Int,
                    ): Page<SearchResult> {
                        delay(1_000)
                        completed += query
                        return Page(emptyList(), 1, 0)
                    }
                }
            val vm = SearchViewModel(catalog, TestLibrary())
            vm.setQuery("antiga")
            advanceTimeBy(400)
            runCurrent()
            vm.setQuery("actual")
            advanceUntilIdle()
            assertEquals(listOf("actual"), completed)
            assertFalse(vm.state.value.loading)
            assertNull(vm.state.value.error)
        }

    @Test
    fun `empty API response remains empty`() =
        runTest(main.dispatcher) {
            val vm = SearchViewModel(DemoCatalogRepository(), TestLibrary())
            vm.setQuery("fixture-not-present")
            advanceUntilIdle()
            assertTrue(vm.state.value.items.isEmpty())
            assertNull(vm.state.value.error)
            assertFalse(vm.state.value.loading)
        }
}
