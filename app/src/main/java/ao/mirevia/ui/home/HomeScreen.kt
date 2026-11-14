package ao.mirevia.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.mirevia.core.model.*
import ao.mirevia.ui.components.*
import ao.mirevia.ui.library.LibraryViewModel
import ao.mirevia.ui.theme.*

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
    val continueItems =
        history
            .filter { item ->
                !item.media.isDemo && entries.none { it.media.key == item.media.key && it.watched }
            }
            .distinctBy { it.media.key }
            .take(12)
    LazyColumn(
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Brand(Modifier.weight(1f))
                IconButton(onClick = viewModel::reload) {
                    Icon(Icons.Outlined.Refresh, "Actualizar início", tint = Muted)
                }
                IconButton(onClick = onSearch) {
                    Icon(Icons.Outlined.Search, "Pesquisar filmes, séries e pessoas")
                }
            }
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 24.dp)) {
                Text(
                    "BOAS HISTÓRIAS, SEM PRESSA",
                    color = Mint,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    "Há sempre algo\npor descobrir.",
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.displaySmall,
                )
            }
        }
        if (isDemo) item { DemoBanner(Modifier.padding(horizontal = 20.dp)) }
        when (val current = state) {
            LoadState.Loading -> item { CatalogSkeleton() }
            is LoadState.Error -> item { ErrorPanel(current.error, viewModel::reload) }
            is LoadState.Data -> {
                val feed = current.value
                feed.featured?.let { media ->
                    item {
                        FeaturedCard(
                            media,
                            entries.any { it.media.key == media.key && it.watchlisted },
                            { onDetails(media.key) },
                            { library.toggle(media, LibraryFlag.WATCHLIST) },
                        )
                    }
                }
                if (continueItems.isNotEmpty())
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.padding(horizontal = 20.dp)) {
                                Text("Continuar a ver", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Players abertos recentemente. Sem progresso comunicado pelo fornecedor.",
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
                items(feed.sections, key = { it.id }) { section ->
                    MediaRow(section.title, section.items, onDetails, onExplore)
                }
                if (feed.sections.all { it.items.isEmpty() })
                    item {
                        StatusPanel(
                            "Nada para mostrar",
                            "O serviço não devolveu conteúdos. Tenta novamente mais tarde.",
                        )
                    }
                if (!isDemo && feed.sections.none { it.id == "recommendations" })
                    item {
                        Surface(
                            Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(
                                Modifier.padding(18.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, null, tint = Mint)
                                Column {
                                    Text(
                                        "Um catálogo mais teu",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        "Adiciona um título à tua biblioteca. Ao actualizar o início, serão apresentadas recomendações da TMDB quando disponíveis.",
                                        color = Muted,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                item {
                    Text(
                        "O teu cinema. A tua lista. No teu dispositivo.",
                        Modifier.padding(horizontal = 20.dp),
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedCard(
    media: MediaSummary,
    watchlisted: Boolean,
    onDetails: () -> Unit,
    onWatchlist: () -> Unit,
) {
    Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))) {
        MediaArtwork(media, Modifier.matchParentSize(), backdrop = true)
        Box(
            Modifier.matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Ink.copy(alpha = 0.04f),
                            Ink.copy(alpha = 0.2f),
                            Ink.copy(alpha = 0.97f),
                        )
                    )
                )
        )
        Column(
            Modifier.fillMaxWidth().padding(22.dp).padding(top = 160.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(color = Color.Black.copy(alpha = 0.35f), shape = RoundedCornerShape(6.dp)) {
                Text(
                    if (media.isDemo) "EM DESTAQUE · DEMONSTRAÇÃO" else "EM DESTAQUE",
                    Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    color = Mint,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Text(media.title, style = MaterialTheme.typography.headlineLarge)
            Text(
                if (media.isDemo) "Conteúdo fictício de exemplo"
                else
                    listOfNotNull(
                            media.key.type.label,
                            media.year?.toString(),
                            media.originalLanguage?.uppercase(),
                        )
                        .joinToString("   ·   "),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFD3DDD9),
            )
            Text(
                media.overview ?: "Informação não disponível",
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD1D3),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDetails,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Icon(Icons.Outlined.Info, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver detalhes", fontWeight = FontWeight.SemiBold)
                }
                FilledTonalIconToggleButton(
                    checked = watchlisted,
                    onCheckedChange = { onWatchlist() },
                    modifier = Modifier.size(50.dp),
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
    Row(
        Modifier.width(300.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MediaArtwork(
            entry.media,
            Modifier.width(66.dp).height(90.dp).clip(RoundedCornerShape(8.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                entry.media.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                if (entry.season != null && entry.episode != null)
                    "T${entry.season} · E${entry.episode}"
                else entry.media.key.type.label,
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Voltar ao player", color = Mint, style = MaterialTheme.typography.labelMedium)
        }
        Icon(Icons.Outlined.PlayCircleOutline, null, tint = Mint)
    }
}
