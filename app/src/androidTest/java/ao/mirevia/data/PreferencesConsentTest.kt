package ao.mirevia.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.platform.app.InstrumentationRegistry
import ao.mirevia.core.playback.IframeRequestPolicy
import ao.mirevia.data.local.PreferencesRepository
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.junit.Assert.*
import org.junit.Test

/** Isolated DataStore files: no catalogue, player network, Room or real user preferences. */
class PreferencesConsentTest {
    private val acceptedKey = booleanPreferencesKey("external_player_consent")
    private val revisionKey = intPreferencesKey("external_player_consent_revision")

    @Test
    fun legacyApprovalIsNotReusedAndOtherPreferencesSurvive() = runBlocking {
        withStore { store, repository ->
            store.edit {
                it[acceptedKey] = true // Format used before the wider HTTPS policy.
                it[booleanPreferencesKey("reduce_motion")] = true
                it[booleanPreferencesKey("save_history")] = false
                it[booleanPreferencesKey("external_browser")] = true
            }
            val preferences = repository.preferences.first()
            assertFalse(preferences.externalPlayerConsent)
            assertTrue(preferences.reduceMotion)
            assertFalse(preferences.saveHistory)
            assertTrue(preferences.externalBrowser)
            assertFalse(preferences.storageError)
            assertNull(store.data.first()[revisionKey])
        }
    }

    @Test
    fun confirmationWritesCurrentRevisionAndRevocationRemovesIt() = runBlocking {
        withStore { store, repository ->
            assertFalse(repository.preferences.first().externalPlayerConsent)
            repository.setPlayerConsent(true)
            assertTrue(repository.preferences.first().externalPlayerConsent)
            assertEquals(true, store.data.first()[acceptedKey])
            assertEquals(IframeRequestPolicy.CONSENT_REVISION, store.data.first()[revisionKey])
            repository.setPlayerConsent(false)
            assertFalse(repository.preferences.first().externalPlayerConsent)
            assertEquals(false, store.data.first()[acceptedKey])
            assertNull(store.data.first()[revisionKey])
        }
    }

    @Test
    fun unknownAndFutureRevisionsFailClosedEvenWithLegacyBoolean() = runBlocking {
        withStore { store, repository ->
            listOf(
                    0,
                    1,
                    IframeRequestPolicy.CONSENT_REVISION - 1,
                    IframeRequestPolicy.CONSENT_REVISION + 1,
                )
                .forEach { revision ->
                    store.edit {
                        it[acceptedKey] = true
                        it[revisionKey] = revision
                    }
                    assertFalse(
                        "revision=$revision",
                        repository.preferences.first().externalPlayerConsent,
                    )
                }
            store.edit {
                it[acceptedKey] = false
                it[revisionKey] = IframeRequestPolicy.CONSENT_REVISION
            }
            assertFalse(repository.preferences.first().externalPlayerConsent)
        }
    }

    @Test
    fun resetClearsBothConsentFields() = runBlocking {
        withStore { store, repository ->
            repository.setPlayerConsent(true)
            repository.reset()
            assertFalse(repository.preferences.first().externalPlayerConsent)
            assertNull(store.data.first()[acceptedKey])
            assertNull(store.data.first()[revisionKey])
        }
    }

    @Test
    fun storageFailureNeverGrantsConsentOrPretendsTheWriteSucceeded() = runBlocking {
        val store =
            object : DataStore<Preferences> {
                override val data: Flow<Preferences> = flow {
                    throw IOException("isolated test failure")
                }

                override suspend fun updateData(
                    transform: suspend (Preferences) -> Preferences
                ): Preferences = throw IOException("isolated test failure")
            }
        val repository = PreferencesRepository(store)
        val preferences = repository.preferences.first()
        assertTrue(preferences.storageError)
        assertFalse(preferences.externalPlayerConsent)
        assertFalse(preferences.saveHistory)
        val writeFailure = runCatching { repository.setPlayerConsent(true) }.exceptionOrNull()
        assertTrue(writeFailure is IOException)
        assertFalse(repository.preferences.first().externalPlayerConsent)
    }

    private suspend fun withStore(
        block: suspend (DataStore<Preferences>, PreferencesRepository) -> Unit
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory =
            File(context.cacheDir, "consent-test-${UUID.randomUUID()}").apply { mkdirs() }
        val job = SupervisorJob()
        val store =
            PreferenceDataStoreFactory.create(
                scope = CoroutineScope(job + Dispatchers.IO),
                produceFile = { File(directory, "consent.preferences_pb") },
            )
        try {
            block(store, PreferencesRepository(store))
        } finally {
            job.cancelAndJoin()
            directory.deleteRecursively()
        }
    }
}
