package ao.mirevia.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.mirevia.core.model.*
import ao.mirevia.ui.components.*
import ao.mirevia.ui.library.LibraryViewModel
import ao.mirevia.ui.theme.Muted
import coil3.compose.AsyncImage

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    library: LibraryViewModel,
    isDemo: Boolean,
    onDetails: (MediaKey) -> Unit,
    onPerson: (Int) -> Unit,
    onExplore: () -> Unit,
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val scope by viewModel.scope.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val searches by library.searches.collectAsStateWithLifecycle()
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Pesquisa", "Filmes, séries, actores e realizadores") }
        if (isDemo) item { DemoBanner(Modifier.padding(horizontal = 20.dp)) }
        item {
            OutlinedTextField(
                query,
                viewModel::setQuery,
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                placeholder = { Text("Que história procuras?") },
                label = { Text("Pesquisar") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = {
                    if (query.isNotEmpty())
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Outlined.Close, "Limpar pesquisa")
                        }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.search(true) }),
            )
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SearchScope.entries) { item ->
                    FilterChip(
                        scope == item,
                        { viewModel.setScope(item) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
        if (notice != null)
            item {
                Text(
                    notice!!,
                    Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        when {
            query.trim().length < 2 -> {
                item {
                    Text(
                        "Escreve pelo menos 2 caracteres. Para género, ano, avaliação ou idioma, utiliza Explorar.",
                        Modifier.padding(horizontal = 20.dp),
                        color = Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                item {
                    TextButton(
                        onClick = onExplore,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Text("Explorar com filtros")
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            null,
                            Modifier.padding(start = 6.dp).size(18.dp),
                        )
                    }
                }
                if (searches.isNotEmpty()) {
                    item {
                        Row(
                            Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Pesquisas recentes",
                                Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            TextButton(onClick = library::clearSearches) { Text("Limpar") }
                        }
                    }
                    items(searches, key = { it }) { recent ->
                        ListItem(
                            headlineContent = { Text(recent) },
                            leadingContent = { Icon(Icons.Outlined.History, null) },
                            modifier = Modifier.clickable { viewModel.setQuery(recent) },
                        )
                    }
                }
            }
            state.loading ->
                items(4) {
                    Skeleton(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(110.dp))
                }
            state.error != null && state.items.isEmpty() ->
                item { ErrorPanel(state.error!!, { viewModel.search(true) }) }
            state.items.isEmpty() ->
                item {
                    StatusPanel(
                        "Sem resultados",
                        if (isDemo)
                            "Só estão disponíveis os títulos fictícios de demonstração. A pesquisa real de pessoas requer a TMDB."
                        else "Não foram encontrados resultados. Experimenta outro título ou nome.",
                        icon = Icons.Outlined.SearchOff,
                    )
                }
            else ->
                items(state.items, key = { it.stableKey }) { result ->
                    when (result) {
                        is SearchResult.Media ->
                            MediaSearchRow(
                                media = result.media,
                                onClick = { onDetails(result.media.key) },
                            )
                        is SearchResult.Person ->
                            PersonSearchRow(result.person) { onPerson(result.person.id) }
                    }
                }
        }
        if (state.error != null && state.items.isNotEmpty())
            item { ErrorPanel(state.error!!, viewModel::loadMore) }
        if (state.hasMore)
            item {
                OutlinedButton(
                    onClick = viewModel::loadMore,
                    enabled = !state.loadingMore,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) {
                    Text(if (state.loadingMore) "A carregar…" else "Carregar mais resultados")
                }
            }
    }
}

@Composable
fun MediaSearchRow(
    media: MediaSummary,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    subtitle: String? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MediaArtwork(media, Modifier.width(70.dp).height(104.dp).clip(RoundedCornerShape(10.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                media.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle
                    ?: if (media.isDemo) "Demonstração · título fictício"
                    else
                        listOfNotNull(media.key.type.label, media.year?.toString())
                            .joinToString(" · "),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (subtitle == null)
                Text(
                    media.overview ?: "Informação não disponível",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
        }
        if (trailing != null) trailing()
        else
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = Muted)
    }
}

@Composable
private fun PersonSearchRow(person: PersonSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(70.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (person.profileUrl != null)
                AsyncImage(
                    person.profileUrl,
                    "Retrato de ${person.name}",
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            else Icon(Icons.Outlined.PersonOutline, null)
        }
        Column(Modifier.weight(1f)) {
            Text(person.name, style = MaterialTheme.typography.titleMedium)
            Text(
                when (person.department) {
                    "Acting" -> "Interpretação"
                    "Directing" -> "Realização"
                    else -> person.department ?: "Pessoa · TMDB"
                },
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = Muted)
    }
}
