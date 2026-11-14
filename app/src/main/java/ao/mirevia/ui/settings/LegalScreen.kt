package ao.mirevia.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ao.mirevia.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LegalScreen(kind: String, onBack: () -> Unit) {
    val title = if (kind == "terms") "Termos de utilização" else "Política de privacidade"
    val filename = if (kind == "terms") "terms.txt" else "privacy.txt"
    val context = LocalContext.current
    val state by
        produceState<LoadState<String>>(LoadState.Loading, filename) {
            value =
                try {
                    LoadState.Data(
                        withContext(Dispatchers.IO) {
                            context.assets.open(filename).bufferedReader().use { it.readText() }
                        }
                    )
                } catch (error: Exception) {
                    LoadState.Error(error.toUiError())
                }
        }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeader(title, onBack = onBack) }
        when (val current = state) {
            LoadState.Loading ->
                item { Skeleton(Modifier.padding(20.dp).fillMaxWidth().height(200.dp)) }
            is LoadState.Error ->
                item {
                    StatusPanel(
                        "Documento indisponível",
                        "Não foi possível ler este documento local.",
                    )
                }
            is LoadState.Data ->
                items(current.value.split("\n\n")) { paragraph ->
                    SelectionContainer {
                        Text(
                            paragraph,
                            Modifier.padding(horizontal = 20.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
        }
    }
}
