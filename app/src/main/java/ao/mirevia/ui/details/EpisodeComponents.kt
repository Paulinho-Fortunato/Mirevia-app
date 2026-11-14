package ao.mirevia.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ao.mirevia.core.model.Season
import ao.mirevia.ui.components.MediaStill
import ao.mirevia.ui.theme.Accent
import ao.mirevia.ui.theme.Muted

internal fun seasonLabel(number: Int) = if (number == 0) "Especiais" else "Temporada $number"

@Composable
fun SeasonSelector(seasons: List<Season>, selected: Int?, onOpen: () -> Unit) {
    val season = seasons.find { it.number == selected }
    OutlinedButton(
        onClick = onOpen,
        enabled = seasons.isNotEmpty(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Outlined.VideoLibrary, null, tint = Accent)
        Column(
            Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                season?.let { seasonLabel(it.number) } ?: "Seleccionar temporada",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                season?.episodeCount?.let { "$it episódios · mudar temporada" }
                    ?: "Escolhe uma temporada disponível na TMDB",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
        }
        Icon(Icons.Outlined.ExpandMore, "Mudar temporada")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeasonPickerSheet(
    seasons: List<Season>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered =
        seasons
            .sortedBy { it.number }
            .filter {
                query.isBlank() ||
                    it.title.contains(query, ignoreCase = true) ||
                    seasonLabel(it.number).contains(query, ignoreCase = true) ||
                    it.number.toString() == query.trim()
            }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.85f).imePadding()) {
            Column(
                Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "CONTINUA A HISTÓRIA",
                    color = Accent,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text("Escolher temporada", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    label = { Text("Procurar temporada") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty())
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Close, "Limpar filtro de temporadas")
                            }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
                Text(
                    "Só a temporada escolhida carrega os episódios. A lista é fornecida pela TMDB, não garante reprodução.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 16.dp)) {
                if (filtered.isEmpty())
                    item {
                        Text(
                            "Nenhuma temporada corresponde à pesquisa.",
                            Modifier.padding(24.dp),
                            color = Muted,
                        )
                    }
                items(filtered, key = { it.number }) { season ->
                    ListItem(
                        headlineContent = { Text(seasonLabel(season.number)) },
                        supportingContent = {
                            Text(
                                listOfNotNull(
                                        season.title.takeUnless {
                                            it.isBlank() || it == seasonLabel(season.number)
                                        },
                                        season.episodeCount?.let { "$it episódios" },
                                        season.airDate?.take(4),
                                    )
                                    .joinToString(" · ")
                                    .ifBlank { "Informação não disponível" },
                                color = Muted,
                            )
                        },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.VideoLibrary,
                                null,
                                tint = if (season.number == selected) Accent else Muted,
                            )
                        },
                        trailingContent = {
                            if (season.number == selected)
                                Icon(Icons.Outlined.Check, "Temporada seleccionada", tint = Accent)
                        },
                        modifier =
                            Modifier.clickable {
                                    onSelect(season.number)
                                    onDismiss()
                                }
                                .heightIn(min = 72.dp),
                        colors =
                            ListItemDefaults.colors(
                                containerColor =
                                    if (season.number == selected) Accent.copy(alpha = 0.08f)
                                    else MaterialTheme.colorScheme.surface
                            ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EpisodeCard(
    item: EpisodeWithSource,
    watched: Boolean,
    onPlay: () -> Unit,
    onWatched: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val episode = item.episode
    var expanded by rememberSaveable(episode.id) { mutableStateOf(false) }
    val largeFont = LocalDensity.current.fontScale > 1.3f
    Surface(
        modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stacked = maxWidth < 290.dp || largeFont
                @Composable
                fun Title() {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "EPISÓDIO ${episode.number.toString().padStart(2, '0')}",
                            color = Accent,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(episode.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(
                                    episode.airDate,
                                    episode.runtimeMinutes?.let { "$it min" },
                                )
                                .joinToString(" · ")
                                .ifBlank { "Informação não disponível" },
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (stacked)
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MediaStill(
                            episode.stillUrl,
                            Modifier.fillMaxWidth()
                                .aspectRatio(16f / 9)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Title()
                    }
                else
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MediaStill(
                            episode.stillUrl,
                            Modifier.width(112.dp)
                                .aspectRatio(16f / 9)
                                .clip(RoundedCornerShape(10.dp)),
                        )
                        Box(Modifier.weight(1f)) { Title() }
                    }
            }
            Text(
                episode.overview ?: "Informação não disponível",
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (episode.overview != null)
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) {
                    Text(if (expanded) "Recolher sinopse" else "Ler sinopse")
                }
            if (item.source == null)
                Text(
                    "Fonte de reprodução indisponível para este episódio.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(
                    onClick = onPlay,
                    enabled = item.source != null,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reproduzir")
                }
                FilterChip(
                    selected = watched,
                    onClick = onWatched,
                    label = { Text(if (watched) "Visto" else "Marcar visto") },
                    leadingIcon = {
                        Icon(Icons.Outlined.CheckCircleOutline, null, Modifier.size(18.dp))
                    },
                    modifier =
                        Modifier.heightIn(min = 48.dp).semantics {
                            contentDescription =
                                if (watched) "Marcar episódio ${episode.number} como não visto"
                                else "Marcar episódio ${episode.number} como visto"
                        },
                )
                IconButton(
                    onClick = onShare,
                    enabled = item.source != null,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Outlined.Share, "Partilhar episódio ${episode.number}")
                }
            }
        }
    }
}
