package ao.mirevia.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.mirevia.core.model.*
import ao.mirevia.core.repository.LibraryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages = _messages.asSharedFlow()
    private val _readError = MutableStateFlow<String?>(null)
    val readError = _readError.asStateFlow()
    val entries =
        repository.entries
            .catch { reportReadError(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val history =
        repository.history
            .catch { reportReadError(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val searches =
        repository.searchHistory
            .catch { reportReadError(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun reportReadError(error: Throwable) {
        if (error is CancellationException) throw error
        _readError.value =
            "Não foi possível ler os dados locais. Fecha a aplicação e tenta novamente."
    }

    fun toggle(media: MediaSummary, flag: LibraryFlag) = mutate { repository.toggle(media, flag) }

    fun remove(key: MediaKey) = mutate { repository.remove(key) }

    fun removeHistory(id: Long) = mutate { repository.removeHistory(id) }

    fun clearHistory() = mutate { repository.clearHistory() }

    fun clearSearches() = mutate { repository.clearSearchHistory() }

    fun markEpisode(key: MediaKey, season: Int, episode: Int, watched: Boolean) = mutate {
        repository.markEpisode(key, season, episode, watched)
    }

    fun clearAll() = mutate { repository.clearAll() }

    private fun mutate(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _messages.emit("Não foi possível actualizar os dados locais. Tenta novamente.")
            }
        }
    }
}
