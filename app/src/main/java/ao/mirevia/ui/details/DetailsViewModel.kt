package ao.mirevia.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.mirevia.core.model.*
import ao.mirevia.core.playback.*
import ao.mirevia.core.repository.*
import ao.mirevia.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
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
    // Small, screen-lifetime metadata cache. No video, history or persistent network storage.
    private val seasonCache = linkedMapOf<Int, List<EpisodeWithSource>>()

    init {
        reload()
    }

    fun reload() {
        val previousSeason = _selectedSeason.value
        detailJob?.cancel()
        seasonJob?.cancel()
        seasonCache.clear()
        _state.value = LoadState.Loading
        _episodes.value = null
        _selectedSeason.value = null
        detailJob =
            viewModelScope.launch {
                try {
                    val details = repository.details(key)
                    val source = resolveSource(details)
                    ensureActive()
                    _state.value = LoadState.Data(DetailsUi(details, source))
                    if (key.type == MediaType.TV) {
                        val seasons = details.seasons.sortedBy { it.number }
                        val initial =
                            seasons.find { it.number == previousSeason }
                                ?: seasons.firstOrNull { it.number > 0 }
                                ?: seasons.firstOrNull()
                        initial?.let { selectSeason(it.number) }
                    }
                } catch (error: Exception) {
                    _state.value = LoadState.Error(error.toUiError())
                }
            }
    }

    fun selectSeason(number: Int) = loadSeason(number, refresh = false)

    fun retrySeason() {
        _selectedSeason.value?.let { loadSeason(it, refresh = true) }
    }

    private fun loadSeason(number: Int, refresh: Boolean) {
        val details = (_state.value as? LoadState.Data)?.value?.details ?: return
        if (key.type != MediaType.TV || details.seasons.none { it.number == number }) return
        seasonJob?.cancel()
        _selectedSeason.value = number
        if (!refresh)
            seasonCache.remove(number)?.let { cached ->
                seasonCache[number] = cached
                _episodes.value = LoadState.Data(cached)
                return
            }
        _episodes.value = LoadState.Loading
        seasonJob =
            viewModelScope.launch {
                try {
                    val result =
                        repository
                            .season(key, number)
                            .filter { it.season == number }
                            .sortedBy { it.number }
                            .map {
                                EpisodeWithSource(it, resolveSource(details, it.season, it.number))
                            }
                    ensureActive()
                    if (_selectedSeason.value == number) {
                        seasonCache.remove(number)
                        seasonCache[number] = result
                        while (seasonCache.size > 3) seasonCache.remove(seasonCache.keys.first())
                        _episodes.value = LoadState.Data(result)
                    }
                } catch (error: Exception) {
                    // toUiError rethrows cancellation; a cancelled season must not replace the next
                    // one.
                    val uiError = error.toUiError()
                    if (_selectedSeason.value == number) _episodes.value = LoadState.Error(uiError)
                }
            }
    }
}
