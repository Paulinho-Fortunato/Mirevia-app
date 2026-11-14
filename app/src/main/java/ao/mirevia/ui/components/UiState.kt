package ao.mirevia.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ao.mirevia.core.model.ConfigurationException
import ao.mirevia.core.model.ContentUnavailableException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>

    data class Data<T>(val value: T) : LoadState<T>

    data class Error(val error: UiError) : LoadState<Nothing>
}

data class UiError(val title: String, val message: String, val offline: Boolean = false)

fun Throwable.toUiError(): UiError {
    if (this is CancellationException) throw this
    return when (this) {
        is ConfigurationException ->
            UiError(
                "Configuração necessária",
                "Verifica a configuração local da TMDB ou do backend. Consulta o README do projecto.",
            )
        is ContentUnavailableException ->
            UiError("Conteúdo indisponível", "Informação não disponível para este conteúdo.")
        is HttpException ->
            when (code()) {
                401,
                403 ->
                    UiError(
                        "Acesso à TMDB indisponível",
                        "Verifica a configuração da API. Não foram utilizados dados de demonstração para substituir esta resposta.",
                    )
                404 ->
                    UiError(
                        "Conteúdo indisponível",
                        "Informação não disponível para este conteúdo.",
                    )
                429 -> UiError("Demasiados pedidos", "Aguarda um pouco antes de tentar novamente.")
                else ->
                    UiError(
                        "Serviço indisponível",
                        "Não foi possível obter os dados. Tenta novamente mais tarde.",
                    )
            }
        is IOException ->
            UiError(
                "Sem ligação ao serviço",
                "Verifica a ligação à Internet e tenta novamente. Os teus favoritos continuam guardados neste dispositivo.",
                true,
            )
        is SerializationException ->
            UiError(
                "Resposta não reconhecida",
                "O serviço devolveu informação que não foi possível ler. Tenta novamente mais tarde.",
            )
        else -> UiError("Não foi possível concluir", "Ocorreu um erro. Tenta novamente.")
    }
}

fun <T : ViewModel> viewModelFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <R : ViewModel> create(modelClass: Class<R>): R = create() as R
    }

data class PageUi<T>(
    val items: List<T> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val page: Int = 0,
    val totalPages: Int = 0,
    val error: UiError? = null,
) {
    val hasMore
        get() = page < totalPages && page < 500
}
