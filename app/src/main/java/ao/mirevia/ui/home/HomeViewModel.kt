package ao.mirevia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.mirevia.core.model.HomeFeed
import ao.mirevia.core.repository.*
import ao.mirevia.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<HomeFeed>>(LoadState.Loading)
    val state = _state.asStateFlow()
    private var job: Job? = null

    init {
        reload()
    }

    fun reload() {
        job?.cancel()
        job =
            viewModelScope.launch {
                _state.value = LoadState.Loading
                try {
                    val seed = library.entries.first().firstOrNull { !it.media.isDemo }?.media?.key
                    _state.value = LoadState.Data(catalog.home(seed))
                } catch (error: Exception) {
                    _state.value = LoadState.Error(error.toUiError())
                }
            }
    }
}
