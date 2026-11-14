package ao.mirevia.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.mirevia.core.model.*
import ao.mirevia.core.repository.CatalogRepository
import ao.mirevia.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ExploreUi(
    val type: MediaType = MediaType.MOVIE,
    val filters: DiscoverFilters = DiscoverFilters(),
    val genres: List<Genre> = emptyList(),
    val results: PageUi<MediaSummary> = PageUi(),
)

class ExploreViewModel(private val repository: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow(ExploreUi())
    val state = _state.asStateFlow()
    private var job: Job? = null

    init {
        reload()
    }

    fun setType(type: MediaType) {
        if (type == _state.value.type) return
        _state.value =
            _state.value.copy(type = type, filters = DiscoverFilters(), genres = emptyList())
        reload()
    }

    fun setFilters(filters: DiscoverFilters) {
        _state.value = _state.value.copy(filters = filters)
        reload()
    }

    fun reload() {
        job?.cancel()
        val snapshot = _state.value
        _state.value = snapshot.copy(results = PageUi(loading = true))
        job =
            viewModelScope.launch {
                try {
                    val genres = repository.genres(snapshot.type)
                    val page = repository.discover(snapshot.type, snapshot.filters, 1)
                    _state.value =
                        snapshot.copy(
                            genres = genres,
                            results =
                                PageUi(page.items, page = page.page, totalPages = page.totalPages),
                        )
                } catch (error: Exception) {
                    _state.value = _state.value.copy(results = PageUi(error = error.toUiError()))
                }
            }
    }

    fun loadMore() {
        val snapshot = _state.value
        if (snapshot.results.loading || snapshot.results.loadingMore || !snapshot.results.hasMore)
            return
        _state.value =
            snapshot.copy(results = snapshot.results.copy(loadingMore = true, error = null))
        job =
            viewModelScope.launch {
                try {
                    val next =
                        repository.discover(
                            snapshot.type,
                            snapshot.filters,
                            snapshot.results.page + 1,
                        )
                    _state.value =
                        snapshot.copy(
                            results =
                                PageUi(
                                    (snapshot.results.items + next.items).distinctBy { it.key },
                                    page = next.page,
                                    totalPages = next.totalPages,
                                )
                        )
                } catch (error: Exception) {
                    _state.value =
                        snapshot.copy(results = snapshot.results.copy(error = error.toUiError()))
                }
            }
    }
}
