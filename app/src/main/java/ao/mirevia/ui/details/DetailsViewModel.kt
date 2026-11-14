package ao.mirevia.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.mirevia.core.model.*
import ao.mirevia.core.playback.*
import ao.mirevia.core.repository.*
import ao.mirevia.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DetailsUi(val details: MediaDetails, val source: VideoSource?)

data class EpisodeWithSource(val episode: Episode, val source: VideoSource?)

class DetailsViewModel(
    private val key: MediaKey,
    private val repository: CatalogRepository,
    private val resolveSource: ResolveVideoSource,
    library: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<DetailsUi>>(LoadState.Loading)
    val state = _state.asStateFlow()
    private val _episodes = MutableStateFlow<LoadState<List<EpisodeWithSource>>?>(null)
    val episodes = _episodes.asStateFlow()
    private val _selectedSeason = MutableStateFlow<Int?>(null)
    val selectedSeason = _selectedSeason.asStateFlow()
    val episodeWatches =
        library
            .episodeStates(key)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private var detailJob: Job? = null
    private var seasonJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        detailJob?.cancel()
        seasonJob?.cancel()
        _state.value = LoadState.Loading
        _episodes.value = null
        _selectedSeason.value = null
        detailJob =
            viewModelScope.launch {
                try {
                    val details = repository.details(key)
                    _state.value = LoadState.Data(DetailsUi(details, resolveSource(details)))
                } catch (error: Exception) {
                    _state.value = LoadState.Error(error.toUiError())
                }
            }
    }

    fun selectSeason(number: Int) {
        val details = (_state.value as? LoadState.Data)?.value?.details ?: return
        if (details.seasons.none { it.number == number }) return
        seasonJob?.cancel()
        _selectedSeason.value = number
        _episodes.value = LoadState.Loading
        seasonJob =
            viewModelScope.launch {
                try {
                    _episodes.value =
                        LoadState.Data(
                            repository.season(key, number).map {
                                EpisodeWithSource(it, resolveSource(details, it.season, it.number))
                            }
                        )
                } catch (error: Exception) {
                    _episodes.value = LoadState.Error(error.toUiError())
                }
            }
    }
}
