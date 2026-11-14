package ao.mirevia.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.mirevia.core.model.*
import ao.mirevia.ui.components.*
import ao.mirevia.ui.library.LibraryViewModel
import ao.mirevia.ui.theme.*
import coil3.compose.AsyncImage
import java.util.Locale

@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    library: LibraryViewModel,
    onBack: () -> Unit,
    onDetails: (MediaKey) -> Unit,
    onPerson: (Int) -> Unit,
    onPlay: (MediaKey, Int?, Int?) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val entries by library.entries.collectAsStateWithLifecycle()
    val episodes by viewModel.episodes.collectAsStateWithLifecycle()
    val selectedSeason by viewModel.selectedSeason.collectAsStateWithLifecycle()
    val episodeWatches by viewModel.episodeWatches.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var trailer by remember { mutableStateOf<OfficialTrailer?>(null) }
    var externalError by remember { mutableStateOf(false) }
    when (val current = state) {
        LoadState.Loading ->
            Column {
                ScreenHeader("Detalhes", onBack = onBack)
                CatalogSkeleton()
            }
        is LoadState.Error ->
            Column {
                ScreenHeader("Detalhes", onBack = onBack)
                ErrorPanel(current.error, viewModel::reload)
            }
        is LoadState.Data -> {
            val details = current.value.details
            val media = details.media
            val entry = entries.find { it.media.key == media.key }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                item { DetailHero(media, onBack) }
                if (media.isDemo) item { DemoBanner(Modifier.padding(horizontal = 20.dp)) }
                item {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(media.title, style = MaterialTheme.typography.headlineLarge)
                        media.originalTitle
                            ?.takeIf { it.isNotBlank() && it != media.title }
                            ?.let { Text(it, color = Muted) }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            media.rating?.let { rating ->
                                Icon(
                                    Icons.Outlined.Star,
                                    null,
                                    Modifier.size(18.dp),
                                    tint = Color(0xFFDFC58F),
                                )
                                Text(
                                    "${String.format(Locale.ROOT, "%.1f", rating)} / 10",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                            Text(
                                listOfNotNull(
                                        media.year?.toString(),
                                        media.key.type.label,
                                        details.runtimeMinutes?.let { "$it min" },
                                    )
                                    .joinToString(" · "),
                                color = Muted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        if (details.genres.isNotEmpty())
                            Text(
                                details.genres.joinToString(" · ") { it.name },
                                color = Mint,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        Button(
                            onClick = { onPlay(media.key, null, null) },
                            enabled = current.value.source != null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (current.value.source != null) "Reproduzir"
                                else "Reprodução indisponível"
                            )
                        }
                        if (current.value.source == null)
                            Text(
                                if (media.isDemo)
                                    "Os títulos de demonstração não têm fonte de reprodução."
                                else
                                    "Não foi possível encontrar uma fonte de reprodução para este conteúdo.",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        else
                            Text(
                                "O endereço do iframe é válido. A disponibilidade do conteúdo só pode ser confirmada pelo player externo.",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = { library.toggle(media, LibraryFlag.WATCHLIST) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    if (entry?.watchlisted == true) Icons.Outlined.Check
                                    else Icons.Outlined.Add,
                                    null,
                                    Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (entry?.watchlisted == true) "Na minha lista"
                                    else "Minha lista"
                                )
                            }
                            FilledTonalIconToggleButton(
                                checked = entry?.favorite == true,
                                onCheckedChange = { library.toggle(media, LibraryFlag.FAVORITE) },
                            ) {
                                Icon(
                                    if (entry?.favorite == true) Icons.Outlined.Favorite
                                    else Icons.Outlined.FavoriteBorder,
                                    if (entry?.favorite == true) "Remover dos favoritos"
                                    else "Adicionar aos favoritos",
                                )
                            }
                        }
                        FilterChip(
                            entry?.watched == true,
                            { library.toggle(media, LibraryFlag.WATCHED) },
                            label = {
                                Text(
                                    if (entry?.watched == true) "Marcado como visto"
                                    else "Marcar como visto"
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.CheckCircleOutline, null, Modifier.size(18.dp))
                            },
                        )
                    }
                }
                item {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("A história", style = MaterialTheme.typography.titleLarge)
                        Text(
                            media.overview ?: "Informação não disponível",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFCDD3D7),
                        )
                        MetadataLine("Título original", media.originalTitle)
                        MetadataLine("Ano", media.year?.toString())
                        MetadataLine("Data de lançamento", media.releaseDate)
                        MetadataLine(
                            "Avaliação TMDB",
                            media.rating?.let { String.format(Locale.ROOT, "%.1f / 10", it) },
                        )
                        MetadataLine(
                            "Géneros",
                            details.genres.joinToString(", ") { it.name }.ifBlank { null },
                        )
                        MetadataLine("Duração", details.runtimeMinutes?.let { "$it minutos" })
                        MetadataLine(
                            "País de origem",
                            details.countries.joinToString(", ").ifBlank { null },
                        )
                        MetadataLine(
                            "Realização",
                            details.directors.joinToString(", ") { it.name }.ifBlank { null },
                        )
                        if (media.key.type == MediaType.TV)
                            MetadataLine(
                                "Criação",
                                details.creators.joinToString(", ") { it.name }.ifBlank { null },
                            )
                        MetadataLine("TMDB", if (media.isDemo) null else media.key.id.toString())
                        MetadataLine("IMDb", details.imdbId)
                        if (details.trailer != null)
                            OutlinedButton(onClick = { trailer = details.trailer }) {
                                Icon(Icons.Outlined.OpenInNew, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Ver trailer oficial")
                            }
                        else
                            Text(
                                "Trailer oficial: Informação não disponível",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                    }
                }
                if (media.key.type == MediaType.TV) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Temporadas e episódios",
                                Modifier.padding(horizontal = 20.dp),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            if (details.seasons.isEmpty())
                                Text(
                                    "Informação não disponível",
                                    Modifier.padding(horizontal = 20.dp),
                                    color = Muted,
                                )
                            else
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(details.seasons, key = { it.number }) { season ->
                                        FilterChip(
                                            selectedSeason == season.number,
                                            { viewModel.selectSeason(season.number) },
                                            label = {
                                                Text(
                                                    if (season.number == 0) "Especiais"
                                                    else "Temporada ${season.number}"
                                                )
                                            },
                                        )
                                    }
                                }
                            if (selectedSeason == null && details.seasons.isNotEmpty())
                                Text(
                                    "Selecciona uma temporada para carregar os episódios.",
                                    Modifier.padding(horizontal = 20.dp),
                                    color = Muted,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            if (selectedSeason == 0)
                                Text(
                                    "A reprodução de especiais não está documentada pelo fornecedor. Os metadados continuam disponíveis.",
                                    Modifier.padding(horizontal = 20.dp),
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                        }
                    }
                    when (val episodeState = episodes) {
                        LoadState.Loading ->
                            item {
                                Skeleton(
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .height(160.dp)
                                )
                            }
                        is LoadState.Error ->
                            item {
                                ErrorPanel(
                                    episodeState.error,
                                    { selectedSeason?.let(viewModel::selectSeason) },
                                )
                            }
                        is LoadState.Data -> {
                            if (episodeState.value.isEmpty())
                                item {
                                    Text(
                                        "Não foram encontrados episódios para esta temporada.",
                                        Modifier.padding(horizontal = 20.dp),
                                        color = Muted,
                                    )
                                }
                            items(episodeState.value, key = { it.episode.id }) { item ->
                                val watched =
                                    episodeWatches.any {
                                        it.season == item.episode.season &&
                                            it.episode == item.episode.number
                                    }
                                EpisodeCard(
                                    item,
                                    watched,
                                    { onPlay(media.key, item.episode.season, item.episode.number) },
                                    {
                                        library.markEpisode(
                                            media.key,
                                            item.episode.season,
                                            item.episode.number,
                                            !watched,
                                        )
                                    },
                                )
                            }
                        }
                        null -> Unit
                    }
                }
                if (details.cast.isNotEmpty())
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Elenco",
                                Modifier.padding(horizontal = 20.dp),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                items(details.cast.take(24), key = { it.id }) { person ->
                                    PersonCard(person) { onPerson(person.id) }
                                }
                            }
                        }
                    }
                else
                    item {
                        Text(
                            "Elenco: Informação não disponível",
                            Modifier.padding(horizontal = 20.dp),
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                if (details.directors.isNotEmpty() || details.creators.isNotEmpty())
                    item {
                        val people = (details.directors + details.creators).distinctBy { it.id }
                        Column(
                            Modifier.padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                "Por detrás da história",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            people.forEach { person ->
                                TextButton(onClick = { onPerson(person.id) }) {
                                    Text(person.name)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Outlined.OpenInNew, null, Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                if (details.recommendations.isNotEmpty())
                    item { MediaRow("Recomendações da TMDB", details.recommendations, onDetails) }
                if (details.similar.isNotEmpty())
                    item {
                        MediaRow(
                            if (media.isDemo) "Outros exemplos fictícios"
                            else "Conteúdos semelhantes",
                            details.similar,
                            onDetails,
                        )
                    }
            }
        }
    }
    trailer?.let { video ->
        AlertDialog(
            onDismissRequest = { trailer = null },
            title = { Text("Ver trailer oficial?") },
            text = {
                Text(
                    "O trailer será aberto na plataforma original, fora da Mirevia. Aplicam-se as regras de privacidade desse serviço."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        externalError =
                            !openTrustedExternal(
                                context,
                                video.url,
                                setOf("www.youtube.com", "vimeo.com"),
                            )
                        trailer = null
                    }
                ) {
                    Text("Abrir")
                }
            },
            dismissButton = { TextButton(onClick = { trailer = null }) { Text("Cancelar") } },
        )
    }
    if (externalError)
        AlertDialog(
            onDismissRequest = { externalError = false },
            title = { Text("Navegador indisponível") },
            text = {
                Text("Não foi possível abrir este endereço num navegador ou aplicação compatível.")
            },
            confirmButton = { TextButton(onClick = { externalError = false }) { Text("Fechar") } },
        )
}

@Composable
private fun DetailHero(media: MediaSummary, onBack: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(280.dp)) {
        MediaArtwork(media, Modifier.fillMaxSize(), backdrop = true)
        Box(
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Ink.copy(alpha = 0.2f), Color.Transparent, Ink))
                )
        )
        FilledIconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            colors =
                IconButtonDefaults.filledIconButtonColors(containerColor = Ink.copy(alpha = 0.7f)),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar")
        }
        MediaArtwork(
            media,
            Modifier.align(Alignment.BottomStart)
                .padding(start = 20.dp)
                .width(104.dp)
                .height(156.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
    }
}

@Composable
private fun MetadataLine(label: String, value: String?) {
    Text(
        "$label: ${value ?: "Informação não disponível"}",
        color = Muted,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun PersonCard(person: PersonSummary, onClick: () -> Unit) {
    Column(
        Modifier.width(100.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(100.dp)
                .clip(CircleShape)
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
            else Icon(Icons.Outlined.PersonOutline, null, Modifier.size(32.dp))
        }
        Text(
            person.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            person.role ?: "Informação não disponível",
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EpisodeCard(
    item: EpisodeWithSource,
    watched: Boolean,
    onPlay: () -> Unit,
    onWatched: () -> Unit,
) {
    val episode = item.episode
    Surface(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "${episode.number}. ${episode.title}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                listOfNotNull(episode.airDate, episode.runtimeMinutes?.let { "$it min" })
                    .joinToString(" · ")
                    .ifBlank { "Informação não disponível" },
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                episode.overview ?: "Informação não disponível",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onPlay,
                    enabled = item.source != null,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp))
                    Text("Reproduzir")
                }
                FilterChip(
                    watched,
                    onWatched,
                    label = { Text(if (watched) "Visto" else "Marcar visto") },
                    leadingIcon = {
                        Icon(Icons.Outlined.CheckCircleOutline, null, Modifier.size(16.dp))
                    },
                )
            }
        }
    }
}
