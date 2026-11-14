package ao.mirevia.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.mirevia.app.AppContainer
import ao.mirevia.core.model.*
import ao.mirevia.core.playback.PlaybackBlock
import ao.mirevia.core.playback.VideoSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class PlayerPhase {
    RESOLVING,
    AVAILABLE,
    UNAVAILABLE,
    ERROR,
    BLOCKED,
}

data class PlayerUi(
    val phase: PlayerPhase = PlayerPhase.RESOLVING,
    val details: MediaDetails? = null,
    val source: VideoSource? = null,
    val wrapperLoading: Boolean = true,
    val reason: String? = null,
    val block: PlaybackBlock? = null,
    val storageNotice: String? = null,
    val attempt: Int = 0,
)

class PlayerViewModel(
    private val key: MediaKey?,
    private val season: Int?,
    private val episode: Int?,
    private val container: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerUi())
    val state = _state.asStateFlow()
    private var resolveJob: Job? = null
    private var sessionCounter = 0L
    val episodeLabel: String? =
        if (season != null && episode != null && season > 0 && episode > 0)
            "Temporada $season · Episódio $episode"
        else null

    private class Session(var openedJob: Job? = null, var historyId: Long? = null)

    private val sessions = mutableMapOf<Long, Session>()

    init {
        retry()
    }

    fun retry() {
        resolveJob?.cancel()
        val attempt = _state.value.attempt + 1
        _state.value = PlayerUi(attempt = attempt)
        resolveJob =
            viewModelScope.launch {
                if (key == null || key.origin != DataOrigin.TMDB) {
                    _state.value = PlayerUi(PlayerPhase.UNAVAILABLE, attempt = attempt)
                    return@launch
                }
                try {
                    val details = container.catalog.details(key)
                    val source = container.resolveVideoSource(details, season, episode)
                    _state.value =
                        PlayerUi(
                            if (source == null) PlayerPhase.UNAVAILABLE else PlayerPhase.AVAILABLE,
                            details,
                            source,
                            attempt = attempt,
                        )
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    _state.value =
                        PlayerUi(
                            PlayerPhase.ERROR,
                            reason =
                                "Não foi possível obter os metadados necessários. Verifica a ligação e a configuração da TMDB.",
                            attempt = attempt,
                        )
                }
            }
    }

    fun wrapperLoaded(attempt: Int) {
        _state.update {
            if (it.attempt == attempt && it.phase == PlayerPhase.AVAILABLE)
                it.copy(wrapperLoading = false)
            else it
        }
    }

    fun failed() = failAttempt(_state.value.attempt)

    fun failAttempt(attempt: Int) {
        _state.update {
            if (it.attempt == attempt && it.phase == PlayerPhase.AVAILABLE)
                it.copy(phase = PlayerPhase.ERROR, wrapperLoading = false)
            else it
        }
    }

    fun blocked(attempt: Int, diagnostic: PlaybackBlock) {
        _state.update {
            if (it.attempt == attempt)
                it.copy(phase = PlayerPhase.BLOCKED, wrapperLoading = false, block = diagnostic)
            else it
        }
    }

    fun grantConsent() {
        viewModelScope.launch {
            try {
                container.preferences.setPlayerConsent(true)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(
                        storageNotice =
                            "Não foi possível guardar o consentimento. O iframe não foi carregado."
                    )
                }
            }
        }
    }

    /** Called only when a consented WebView is actually created. Does not assert video playback. */
    fun sessionOpened(): Long {
        _state.update { it.copy(wrapperLoading = true) }
        val token = ++sessionCounter
        val details = _state.value.details ?: return token
        val session = Session()
        sessions[token] = session
        session.openedJob =
            container.applicationScope.launch {
                try {
                    if (container.preferences.preferences.first().saveHistory)
                        session.historyId =
                            container.library.recordOpened(details.media, season, episode)
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    _state.update {
                        it.copy(
                            storageNotice =
                                "Não foi possível guardar a abertura do player no histórico local."
                        )
                    }
                }
            }
        return token
    }

    fun sessionClosed(token: Long) {
        val session = sessions.remove(token) ?: return
        container.applicationScope.launch {
            session.openedJob?.join()
            try {
                session.historyId?.let { container.library.recordLeft(it) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _state.update {
                    it.copy(
                        storageNotice =
                            "Não foi possível guardar a saída do player no histórico local."
                    )
                }
            }
        }
    }

    override fun onCleared() {
        sessions.keys.toList().forEach(::sessionClosed)
    }
}
