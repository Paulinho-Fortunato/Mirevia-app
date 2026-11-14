package ao.mirevia.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import ao.mirevia.core.model.*
import ao.mirevia.data.local.MireviaDatabase
import ao.mirevia.data.repository.RoomLibraryRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RoomLibraryRepositoryTest {
    private lateinit var db: MireviaDatabase
    private lateinit var library: RoomLibraryRepository

    @Before
    fun setup() {
        db =
            Room.inMemoryDatabaseBuilder(
                    InstrumentationRegistry.getInstrumentation().targetContext,
                    MireviaDatabase::class.java,
                )
                .build()
        library = RoomLibraryRepository(db)
    }

    @After
    fun close() {
        db.close()
    }

    private fun media(origin: DataOrigin = DataOrigin.DEMO, type: MediaType = MediaType.MOVIE) =
        MediaSummary(MediaKey(origin, type, 1), "Fixture")

    @Test
    fun favoriteAndWatchlistFlagsAreIndependent() = runBlocking {
        val item = media()
        library.toggle(item, LibraryFlag.FAVORITE)
        library.toggle(item, LibraryFlag.WATCHLIST)
        library.toggle(item, LibraryFlag.FAVORITE)
        assertFalse(library.entries.first().single().favorite)
        assertTrue(library.entries.first().single().watchlisted)
        library.toggle(item, LibraryFlag.WATCHLIST)
        assertTrue(library.entries.first().isEmpty())
    }

    @Test
    fun originAndMediaTypePreventIdCollisions() = runBlocking {
        for (item in listOf(media(), media(DataOrigin.TMDB), media(type = MediaType.TV))) library
            .toggle(item, LibraryFlag.FAVORITE)
        assertEquals(3, library.entries.first().size)
    }

    @Test
    fun historyNeverInventsProgressOrCompletion() = runBlocking {
        val id = library.recordOpened(media(), null, null)
        library.recordLeft(id)
        val entry = library.history.first().single()
        assertEquals(PlaybackStatus.LEFT, entry.status)
        assertNull(entry.positionMillis)
        assertNull(entry.durationMillis)
        assertNotNull(entry.leftAt)
        library.clearHistory()
        assertTrue(library.history.first().isEmpty())
    }

    @Test
    fun watchedEpisodesRemainLocalAndCanBeRemoved() = runBlocking {
        val key = media(type = MediaType.TV).key
        library.markEpisode(key, 1, 2, true)
        assertEquals(2, library.episodeStates(key).first().single().episode)
        library.markEpisode(key, 1, 2, false)
        assertTrue(library.episodeStates(key).first().isEmpty())
    }

    @Test
    fun searchesAreDeduplicatedAndBounded() = runBlocking {
        repeat(25) { library.rememberSearch("Fixture $it") }
        assertEquals(20, library.searchHistory.first().size)
        library.rememberSearch("FIXTURE 24")
        assertEquals(20, library.searchHistory.first().size)
        library.clearAll()
        assertTrue(library.searchHistory.first().isEmpty())
    }
}
