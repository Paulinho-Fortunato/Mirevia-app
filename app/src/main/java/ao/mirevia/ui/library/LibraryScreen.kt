package ao.mirevia.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.mirevia.core.model.*
import ao.mirevia.ui.components.ScreenHeader
import ao.mirevia.ui.components.StatusPanel
import ao.mirevia.ui.search.MediaSearchRow
import ao.mirevia.ui.theme.Muted
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LibraryScreen(viewModel: LibraryViewModel, onDetails: (MediaKey) -> Unit) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val readError by viewModel.readError.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }
    val tabs = listOf("Minha lista", "Favoritos", "Vistos", "Histórico")
    val selected =
        entries.filter {
            when (tab) {
                0 -> it.watchlisted
                1 -> it.favorite
                2 -> it.watched
                else -> false
            }
        }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("A tua biblioteca", "Só tua. Guardada neste dispositivo.") }
        if (readError != null)
            item {
                Text(
                    readError!!,
                    Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(tabs.size) { index ->
                    FilterChip(tab == index, { tab = index }, label = { Text(tabs[index]) })
                }
            }
        }
        if (tab == 3) {
            item {
                Text(
                    "O histórico regista a abertura do player, não a duração vista nem a conclusão do vídeo.",
                    Modifier.padding(horizontal = 20.dp),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                if (history.isNotEmpty())
                    TextButton(
                        onClick = { confirmClear = true },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Text("Limpar histórico")
                    }
            }
            if (history.isEmpty())
                item {
                    StatusPanel(
                        "Ainda sem histórico",
                        "Os players que abrires vão aparecer aqui. Podes desactivar este registo nas definições.",
                        icon = Icons.Outlined.History,
                    )
                }
            items(history, key = { it.id }) { item ->
                val date =
                    remember(item.openedAt) {
                        DateTimeFormatter.ofPattern(
                                "dd MMM · HH:mm",
                                Locale.forLanguageTag("pt-PT"),
                            )
                            .format(
                                Instant.ofEpochMilli(item.openedAt).atZone(ZoneId.systemDefault())
                            )
                    }
                MediaSearchRow(
                    item.media,
                    { onDetails(item.media.key) },
                    trailing = {
                        IconButton(onClick = { viewModel.removeHistory(item.id) }) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                "Remover ${item.media.title} do histórico",
                            )
                        }
                    },
                    subtitle =
                        listOfNotNull(
                                item.season?.let { "T$it · E${item.episode}" },
                                "Player aberto · $date",
                            )
                            .joinToString("\n"),
                )
            }
        } else {
            if (selected.isEmpty())
                item {
                    StatusPanel(
                        when (tab) {
                            0 -> "A tua próxima sessão começa aqui"
                            1 -> "Guarda o que mais gostas"
                            else -> "As histórias que já conheces"
                        },
                        when (tab) {
                            0 -> "Adiciona filmes e séries à tua lista a partir dos detalhes."
                            1 -> "Marca um título como favorito para o encontrares facilmente."
                            else ->
                                "Marca conteúdos como vistos. A aplicação não adivinha quando terminas um vídeo."
                        },
                        icon =
                            when (tab) {
                                0 -> Icons.Outlined.Bookmarks
                                1 -> Icons.Outlined.FavoriteBorder
                                else -> Icons.Outlined.CheckCircleOutline
                            },
                    )
                }
            items(selected, key = { it.media.key.storageKey }) { entry ->
                MediaSearchRow(
                    entry.media,
                    { onDetails(entry.media.key) },
                    trailing = {
                        IconButton(
                            onClick = {
                                viewModel.toggle(
                                    entry.media,
                                    when (tab) {
                                        0 -> LibraryFlag.WATCHLIST
                                        1 -> LibraryFlag.FAVORITE
                                        else -> LibraryFlag.WATCHED
                                    },
                                )
                            }
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                "Remover ${entry.media.title} de ${tabs[tab]}",
                            )
                        }
                    },
                )
            }
        }
    }
    if (confirmClear)
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Limpar histórico?") },
            text = {
                Text(
                    "Esta acção remove todos os registos de abertura de players deste dispositivo."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        confirmClear = false
                    }
                ) {
                    Text("Limpar")
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
}
