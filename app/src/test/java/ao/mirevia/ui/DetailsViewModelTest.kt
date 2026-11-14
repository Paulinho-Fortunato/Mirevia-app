package ao.mirevia.ui

import ao.mirevia.core.model.*
import ao.mirevia.core.playback.*
import ao.mirevia.core.repository.*
import ao.mirevia.data.demo.DemoCatalogRepository
import ao.mirevia.ui.components.LoadState
import ao.mirevia.ui.details.*
import java.io.IOException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val key = MediaKey(DataOrigin.TMDB, MediaType.TV, 30984)
    private val details =
        MediaDetails(
            MediaSummary(key, "Série fictícia para teste local"),
            seasons =
                (0..4).map { Season(it + 100, it, "Temporada de teste $it", episodeCount = 1) },
        )
    private val policy = EmbedUrlPolicy(setOf("myembed.biz"))
    private val resolver = ResolveVideoSource(EmbedMoviesProvider(policy.allowedHosts), policy)

    private fun episode(number: Int) =
        Episode(1000 + number, number, 1, "Episódio fictício", overview = null)

    private fun catalog(load: suspend (Int) -> List<Episode>) =
        object : CatalogRepository by DemoCatalogRepository() {
            override suspend fun details(key: MediaKey) =
                this@DetailsViewModelTest.details.let { it.copy(media = it.media.copy(key = key)) }

            override suspend fun season(key: MediaKey, number: Int) = load(number)
        }

    private fun vm(repository: CatalogRepository, mediaKey: MediaKey = key) =
        DetailsViewModel(mediaKey, repository, resolver, TestLibrary())

    private fun rows(vm: DetailsViewModel) =
        (vm.episodes.value as LoadState.Data<List<EpisodeWithSource>>).value

    @Test
    fun `loads only the first regular season and builds its real episode route`() =
        runTest(main.dispatcher) {
            val calls = mutableListOf<Int>()
            val vm =
                vm(
                    catalog {
                        calls += it
                        listOf(episode(it))
                    }
                )
            advanceUntilIdle()
            assertEquals(listOf(1), calls)
            assertEquals(1, vm.selectedSeason.value)
            assertEquals("https://myembed.biz/serie/30984/1/1", rows(vm).single().source?.url)
        }

    @Test
    fun `switching seasons is lazy and revisiting uses bounded screen metadata cache`() =
        runTest(main.dispatcher) {
            val calls = mutableListOf<Int>()
            val vm =
                vm(
                    catalog {
                        calls += it
                        listOf(episode(it))
                    }
                )
            advanceUntilIdle()
            vm.selectSeason(2)
            advanceUntilIdle()
            assertEquals("https://myembed.biz/serie/30984/2/1", rows(vm).single().source?.url)
            vm.selectSeason(1)
            advanceUntilIdle()
            assertEquals(listOf(1, 2), calls)
            vm.selectSeason(3)
            advanceUntilIdle()
            vm.selectSeason(4)
            advanceUntilIdle()
            vm.selectSeason(2)
            advanceUntilIdle()
            assertEquals(listOf(1, 2, 3, 4, 2), calls)
        }

    @Test
    fun `cancelled noncooperative season cannot replace the latest selection`() =
        runTest(main.dispatcher) {
            val vm =
                vm(
                    catalog { number ->
                        if (number == 1) withContext(NonCancellable) { delay(1_000) }
                        listOf(episode(number))
                    }
                )
            runCurrent()
            vm.selectSeason(2)
            advanceUntilIdle()
            assertEquals(2, vm.selectedSeason.value)
            assertEquals(2, rows(vm).single().episode.season)
        }

    @Test
    fun `failed selection can retry without demo fallback`() =
        runTest(main.dispatcher) {
            var failures = true
            val vm =
                vm(
                    catalog {
                        if (it == 2 && failures) throw IOException("fixture failure")
                        listOf(episode(it))
                    }
                )
            advanceUntilIdle()
            vm.selectSeason(2)
            advanceUntilIdle()
            assertTrue(vm.episodes.value is LoadState.Error)
            failures = false
            vm.retrySeason()
            advanceUntilIdle()
            assertEquals(2, rows(vm).single().episode.season)
        }

    @Test
    fun `specials keep metadata but never invent a supported playback source`() =
        runTest(main.dispatcher) {
            val vm = vm(catalog { listOf(episode(it)) })
            advanceUntilIdle()
            vm.selectSeason(0)
            advanceUntilIdle()
            assertEquals(0, rows(vm).single().episode.season)
            assertNull(rows(vm).single().source)
        }

    @Test
    fun `unknown seasons are ignored and empty seasons remain empty`() =
        runTest(main.dispatcher) {
            val calls = mutableListOf<Int>()
            val vm =
                vm(
                    catalog {
                        calls += it
                        emptyList()
                    }
                )
            advanceUntilIdle()
            vm.selectSeason(99)
            advanceUntilIdle()
            assertEquals(listOf(1), calls)
            assertEquals(1, vm.selectedSeason.value)
            assertTrue(rows(vm).isEmpty())
        }

    @Test
    fun `reload retains a valid selected season and clears its old cache`() =
        runTest(main.dispatcher) {
            val calls = mutableListOf<Int>()
            val vm =
                vm(
                    catalog {
                        calls += it
                        listOf(episode(it))
                    }
                )
            advanceUntilIdle()
            vm.selectSeason(3)
            advanceUntilIdle()
            vm.reload()
            advanceUntilIdle()
            assertEquals(3, vm.selectedSeason.value)
            assertEquals(listOf(1, 3, 3), calls)
        }

    @Test
    fun `movies never fetch a season list`() =
        runTest(main.dispatcher) {
            val calls = mutableListOf<Int>()
            val vm =
                vm(
                    catalog {
                        calls += it
                        listOf(episode(it))
                    },
                    key.copy(type = MediaType.MOVIE),
                )
            advanceUntilIdle()
            vm.selectSeason(1)
            advanceUntilIdle()
            assertTrue(calls.isEmpty())
            assertNull(vm.selectedSeason.value)
            assertNull(vm.episodes.value)
        }

    private class TestLibrary : LibraryRepository {
        override val entries = flowOf(emptyList<LibraryEntry>())
        override val history = flowOf(emptyList<PlaybackEntry>())
        override val searchHistory = flowOf(emptyList<String>())

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

        override suspend fun rememberSearch(query: String) = Unit

        override suspend fun clearSearchHistory() = Unit

        override suspend fun clearAll() = Unit
    }
}
