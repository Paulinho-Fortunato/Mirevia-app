package ao.mirevia.ui.explore

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.mirevia.core.model.*
import ao.mirevia.ui.components.*
import ao.mirevia.ui.theme.Muted

@Composable
fun ExploreScreen(viewModel: ExploreViewModel, isDemo: Boolean, onDetails: (MediaKey) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(144.dp),
        state = gridState,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader("Explorar", "Encontra histórias à tua medida")
        }
        if (isDemo) item(span = { GridItemSpan(maxLineSpan) }) { DemoBanner() }
        item(span = { GridItemSpan(maxLineSpan) }) {
            ExploreFilters(state, viewModel::setType, viewModel::setFilters)
        }
        when {
            state.results.loading -> items(6) { Skeleton(Modifier.fillMaxWidth().height(230.dp)) }
            state.results.error != null && state.results.items.isEmpty() ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ErrorPanel(state.results.error!!, viewModel::reload)
                }
            state.results.items.isEmpty() ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    StatusPanel(
                        "Sem resultados",
                        if (isDemo)
                            "Os exemplos não possuem géneros, anos ou avaliações reais. Limpa os filtros ou configura a TMDB."
                        else "Não foram encontrados conteúdos para estes filtros.",
                        action = "Limpar filtros",
                        onAction = { viewModel.setFilters(DiscoverFilters()) },
                    )
                }
            else ->
                items(state.results.items, key = { it.key.storageKey }) { media ->
                    MediaCard(media, { onDetails(media.key) }, Modifier.fillMaxWidth())
                }
        }
        if (state.results.items.isNotEmpty() && state.results.error != null)
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorPanel(state.results.error!!, viewModel::loadMore)
            }
        if (state.results.hasMore)
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedButton(
                    onClick = viewModel::loadMore,
                    enabled = !state.results.loadingMore,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.results.loadingMore) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(if (state.results.loadingMore) "A carregar…" else "Carregar mais")
                }
            }
    }
    LaunchedEffect(gridState, state.results.page, state.results.hasMore) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                if (
                    state.results.items.isNotEmpty() &&
                        last >= gridState.layoutInfo.totalItemsCount - 4 &&
                        state.results.error == null
                )
                    viewModel.loadMore()
            }
    }
}

@Composable
private fun ExploreFilters(
    state: ExploreUi,
    onType: (MediaType) -> Unit,
    onFilters: (DiscoverFilters) -> Unit,
) {
    var year by
        rememberSaveable(state.type, state.filters.year) {
            mutableStateOf(state.filters.year?.toString().orEmpty())
        }
    val yearValid = year.isBlank() || year.toIntOrNull()?.let { it in 1870..2200 } == true
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MediaType.entries.forEach { type ->
                FilterChip(
                    selected = state.type == type,
                    onClick = { onType(type) },
                    label = { Text(if (type == MediaType.MOVIE) "Filmes" else "Séries") },
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                ChoiceMenu<Int?>(
                    "Género",
                    state.filters.genreId,
                    listOf(null to "Todos") + state.genres.map { it.id to it.name },
                    { onFilters(state.filters.copy(genreId = it)) },
                )
            }
            item {
                ChoiceMenu<Int?>(
                    "Avaliação",
                    state.filters.minimumRating,
                    listOf(null to "Todas", 6 to "6+", 7 to "7+", 8 to "8+", 9 to "9+"),
                    { onFilters(state.filters.copy(minimumRating = it)) },
                )
            }
            item {
                ChoiceMenu<String?>(
                    "Idioma original",
                    state.filters.originalLanguage,
                    listOf(
                        null to "Todos",
                        "pt" to "Português",
                        "en" to "Inglês",
                        "es" to "Espanhol",
                        "fr" to "Francês",
                        "ja" to "Japonês",
                    ),
                    { onFilters(state.filters.copy(originalLanguage = it)) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                year,
                onValueChange = { year = it.filter(Char::isDigit).take(4) },
                modifier = Modifier.width(110.dp),
                label = { Text("Ano") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !yearValid,
            )
            OutlinedButton(
                onClick = { onFilters(state.filters.copy(year = year.toIntOrNull())) },
                enabled = yearValid,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("Aplicar ano")
            }
        }
        if (!yearValid)
            Text(
                "Introduz um ano válido, entre 1870 e 2200.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        Text(
            "Filtros aplicados ao catálogo TMDB. As datas e avaliações não confirmam a disponibilidade no player externo.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
