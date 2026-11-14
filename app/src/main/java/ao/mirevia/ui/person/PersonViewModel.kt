package ao.mirevia.ui.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ao.mirevia.core.model.PersonDetails
import ao.mirevia.core.repository.CatalogRepository
import ao.mirevia.ui.components.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PersonViewModel(private val id: Int, private val catalog: CatalogRepository) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<PersonDetails>>(LoadState.Loading)
    val state = _state.asStateFlow()
    private var job: Job? = null

    init {
        reload()
    }

    fun reload() {
        job?.cancel()
        _state.value = LoadState.Loading
        job =
            viewModelScope.launch {
                try {
                    _state.value = LoadState.Data(catalog.person(id))
                } catch (error: Exception) {
                    _state.value = LoadState.Error(error.toUiError())
                }
            }
    }
}
