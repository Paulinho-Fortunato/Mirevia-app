package ao.mirevia.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.mireviaPreferences by preferencesDataStore("mirevia_preferences")

data class UserPreferences(
    val reduceMotion: Boolean = false,
    val saveHistory: Boolean = true,
    val externalBrowser: Boolean = false,
    val externalPlayerConsent: Boolean = false,
    val storageError: Boolean = false,
)

class PreferencesRepository(context: Context) {
    private val store = context.applicationContext.mireviaPreferences
    val preferences =
        store.data
            .map {
                UserPreferences(
                    it[REDUCE_MOTION] ?: false,
                    it[SAVE_HISTORY] ?: true,
                    it[EXTERNAL_BROWSER] ?: false,
                    it[PLAYER_CONSENT] ?: false,
                )
            }
            .catch { error ->
                if (error is IOException)
                    emit(UserPreferences(saveHistory = false, storageError = true))
                else throw error
            }

    suspend fun setReduceMotion(value: Boolean) {
        store.edit { it[REDUCE_MOTION] = value }
    }

    suspend fun setSaveHistory(value: Boolean) {
        store.edit { it[SAVE_HISTORY] = value }
    }

    suspend fun setExternalBrowser(value: Boolean) {
        store.edit { it[EXTERNAL_BROWSER] = value }
    }

    suspend fun setPlayerConsent(value: Boolean) {
        store.edit { it[PLAYER_CONSENT] = value }
    }

    suspend fun reset() {
        store.edit { it.clear() }
    }

    private companion object {
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val EXTERNAL_BROWSER = booleanPreferencesKey("external_browser")
        val PLAYER_CONSENT = booleanPreferencesKey("external_player_consent")
    }
}
