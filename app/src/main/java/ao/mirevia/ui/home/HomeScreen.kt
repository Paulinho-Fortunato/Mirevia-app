package ao.mirevia.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.mirevia.core.model.*
import ao.mirevia.ui.components.*
import ao.mirevia.ui.library.LibraryViewModel
import ao.mirevia.ui.theme.*
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    library: LibraryViewModel,
    isDemo: Boolean,
    onDetails: (MediaKey) -> Unit,
    onSearch: () -> Unit,
    onExplore: () -> Unit,
    onContinue: (PlaybackEntry) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val entries by library.entries.collectAsStateWithLifecycle()
    val history by library.history.collectAsStateWithLifecycle()
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedType = type?.let { MediaType.valueOf(it) }
    val continueItems =
        history
            .filter { item ->
                !item.media.isDemo &&
                    entries.none { it.media.key == item.media.key && it.watched } &&
                    (selectedType == null || item.media.key.type == selectedType)
            }
            .distinctBy { it.media.key }
            .take(12)
    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item(key = "brand") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Brand(Modifier.weight(1f))
                IconButton(onClick = viewModel::reload) {
                    Icon(Icons.Outlined.Refresh, "Actualizar início", tint = Muted)
                }
                FilledTonalIconButton(onClick = onSearch, shape = CircleShape) {
                    Icon(Icons.Outlined.Search, "Pesquisar filmes, séries e pessoas")
                }
            }
            Text(
                "O TEU PRÓXIMO GRANDE ENCONTRO",
                Modifier.padding(start = 20.dp, top = 20.dp),
                color = Muted,
                style = MaterialTheme.typography.labelSmall,
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = type == null,
                        onClick = { type = null },
                        label = { Text("Destaques") },
                    )
                }
                items(MediaType.entries) { item ->
                    FilterChip(
                        selected = type == item.name,
                        onClick = { type = item.name },
                        label = { Text(if (item == MediaType.MOVIE) "Filmes" else "Séries") },
                    )
                }
                item {
                    AssistChip(
                        onClick = onExplore,
                        label = { Text("Todo o catálogo") },
                        trailingIcon = { Icon(Icons.Outlined.Tune, null, Modifier.size(16.dp)) },
                    )
                }
            }
            if (type != null)
                Text(
                    "A mostrar este tipo nos destaques carregados. Para o catálogo completo, usa Explorar.",
                    Modifier.padding(horizontal = 20.dp),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
        }
        if (isDemo) item { DemoBanner(Modifier.padding(horizontal = 20.dp)) }
        when (val current = state) {
            LoadState.Loading -> item { CatalogSkeleton() }
            is LoadState.Error -> item { ErrorPanel(current.error, viewModel::reload) }
            is LoadState.Data -> {
                val feed = current.value
                val sections =
                    feed.sections
                        .map { section ->
                            section.copy(
                                items =
                                    section.items.filter {
                                        selectedType == null || it.key.type == selectedType
                                    }
                            )
                        }
                        .filter { it.items.isNotEmpty() }
                val featured =
                    feed.featured?.takeIf { selectedType == null || it.key.type == selectedType }
                        ?: sections.firstOrNull()?.items?.firstOrNull()
                featured?.let { media ->
                    item(key = "feature-${media.key.storageKey}") {
                        FeaturedCard(
                            media = media,
                            watchlisted =
                                entries.any { it.media.key == media.key && it.watchlisted },
                            onDetails = { onDetails(media.key) },
                            onWatchlist = { library.toggle(media, LibraryFlag.WATCHLIST) },
                        )
                    }
                }
                if (continueItems.isNotEmpty())
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Column(
                                Modifier.padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text(
                                    "Players recentes",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    "Volta a abrir. O fornecedor não comunica progresso nem posição de retoma.",
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                items(continueItems, key = { it.id }) { item ->
                                    ContinueCard(item) { onContinue(item) }
                                }
                            }
                        }
                    }
                items(sections, key = { it.id }) { section ->
                    MediaRow(section.title, section.items, onDetails, onExplore)
                }
                if (sections.isEmpty() && featured == null)
                    item {
                        StatusPanel(
                            "Nada para mostrar",
                            "Não há títulos deste tipo nos destaques carregados.",
                            action = "Explorar catálogo",
                            onAction = onExplore,
                        )
                    }
                if (!isDemo && sections.none { it.id == "recommendations" })
                    item {
                        Surface(
                            Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Row(
                                Modifier.padding(20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Icon(Icons.Outlined.Bookmarks, null, tint = Accent)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "As boas histórias ficam contigo.",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        "Guarda títulos na tua lista. Ao actualizar o início, verás recomendações da TMDB quando disponíveis.",
                                        color = Muted,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                item {
                    Row(
                        Modifier.padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(Accent))
                        Text(
                            "O teu cinema. A tua lista. No teu dispositivo.",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FeaturedCard(
    media: MediaSummary,
    watchlisted: Boolean,
    onDetails: () -> Unit,
    onWatchlist: () -> Unit,
) {
    Box(
        Modifier.padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(26.dp))
    ) {
        MediaArtwork(media, Modifier.matchParentSize(), backdrop = true)
        Box(
            Modifier.matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Ink.copy(alpha = 0.04f),
                        0.35f to Ink.copy(alpha = 0.65f),
                        1f to Ink.copy(alpha = 0.99f),
                    )
                )
        )
        Box(
            Modifier.matchParentSize()
                .background(
                    Brush.horizontalGradient(listOf(Ink.copy(alpha = 0.3f), Color.Transparent))
                )
        )
        Column(
            Modifier.fillMaxWidth().padding(24.dp).padding(top = 170.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.width(3.dp).height(16.dp).clip(CircleShape).background(Accent))
                Text(
                    if (media.isDemo) "EM DESTAQUE · DEMO" else "EM DESTAQUE",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(
                media.title,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MetadataPill(
                    if (media.isDemo) "Título fictício" else media.key.type.label,
                    highlighted = true,
                )
                if (!media.isDemo) {
                    media.year?.let { MetadataPill(it.toString()) }
                    media.rating?.let {
                        MetadataPill("TMDB ${String.format(Locale.ROOT, "%.1f", it)}")
                    }
                    media.originalLanguage?.let { MetadataPill(it.uppercase(Locale.ROOT)) }
                }
            }
            Text(
                media.overview ?: "Informação não disponível",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color(0xFFDBD7DF),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDetails,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Outlined.Info, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver detalhes")
                }
                FilledTonalIconToggleButton(
                    checked = watchlisted,
                    onCheckedChange = { onWatchlist() },
                    modifier = Modifier.size(52.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        if (watchlisted) Icons.Outlined.Check else Icons.Outlined.Add,
                        if (watchlisted) "Remover da minha lista" else "Adicionar à minha lista",
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueCard(entry: PlaybackEntry, onClick: () -> Unit) {
    Column(
        Modifier.width(250.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9)) {
            MediaArtwork(entry.media, Modifier.fillMaxSize(), backdrop = true)
            Box(
                Modifier.matchParentSize()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(alpha = 0.8f)))
                    )
            )
            Surface(
                Modifier.align(Alignment.Center),
                shape = CircleShape,
                color = Ink.copy(alpha = 0.7f),
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    null,
                    Modifier.padding(10.dp).size(24.dp),
                    tint = Color.White,
                )
            }
            Text(
                if (entry.season != null && entry.episode != null)
                    "T${entry.season} · E${entry.episode}"
                else entry.media.key.type.label,
                Modifier.align(Alignment.BottomStart).padding(12.dp),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                entry.media.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text("Voltar ao player", color = Accent, style = MaterialTheme.typography.bodySmall)
        }
    }
}
