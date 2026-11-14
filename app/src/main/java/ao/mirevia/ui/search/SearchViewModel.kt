package ao.mirevia.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.mirevia.core.model.*
import ao.mirevia.core.repository.*
import ao.mirevia.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SearchViewModel(
    private val catalog: CatalogRepository,
    private val library: LibraryRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _scope = MutableStateFlow(SearchScope.ALL)
    val scope = _scope.asStateFlow()
    private val _state = MutableStateFlow(PageUi<SearchResult>())
    val state = _state.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()
    private var job: Job? = null

    fun setQuery(query: String) {
        _query.value = query.take(200)
        search()
    }

    fun setScope(scope: SearchScope) {
        _scope.value = scope
        search()
    }

    fun search(immediate: Boolean = false) {
        job?.cancel()
        val query = _query.value.trim()
        val scope = _scope.value
        _notice.value = null
        if (query.length < 2) {
            _state.value = PageUi()
            return
        }
        _state.value = PageUi(loading = true)
        job =
            viewModelScope.launch {
                if (!immediate) delay(350)
                try {
                    val page = catalog.search(query, scope, 1)
                    _state.value =
                        PageUi(page.items, page = page.page, totalPages = page.totalPages)
                    try {
                        library.rememberSearch(query)
                    } catch (error: Exception) {
                        if (error is kotlinx.coroutines.CancellationException) throw error
                        _notice.value =
                            "Os resultados foram carregados, mas não foi possível guardar a pesquisa localmente."
                    }
                } catch (error: Exception) {
                    _state.value = PageUi(error = error.toUiError())
                }
            }
    }

    fun loadMore() {
        val previous = _state.value
        if (previous.loading || previous.loadingMore || !previous.hasMore) return
        _state.value = previous.copy(loadingMore = true, error = null)
        val query = _query.value.trim()
        val scope = _scope.value
        job =
            viewModelScope.launch {
                try {
                    val next = catalog.search(query, scope, previous.page + 1)
                    _state.value =
                        PageUi(
                            (previous.items + next.items).distinctBy { it.stableKey },
                            page = next.page,
                            totalPages = next.totalPages,
                        )
                } catch (error: Exception) {
                    _state.value = previous.copy(error = error.toUiError())
                }
            }
    }
}
