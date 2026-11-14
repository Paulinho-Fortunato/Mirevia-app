package ao.mirevia.ui.details

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.mirevia.core.model.*
import ao.mirevia.core.playback.EmbedUrlPolicy
import ao.mirevia.ui.components.*
import ao.mirevia.ui.library.LibraryViewModel
import ao.mirevia.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DetailsScreen(
    viewModel: DetailsViewModel,
    library: LibraryViewModel,
    policy: EmbedUrlPolicy,
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
    var seasonPicker by rememberSaveable { mutableStateOf(false) }
    var shareTarget by remember { mutableStateOf<ShareTarget?>(null) }
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
            val panels =
                if (media.key.type == MediaType.TV) listOf("Episódios", "Sobre", "Mais títulos")
                else listOf("Sobre", "Mais títulos")
            var panel by rememberSaveable(media.key.storageKey) { mutableStateOf(panels.first()) }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                item(key = "hero") {
                    DetailHero(
                        media,
                        onBack,
                        onShare = { shareTarget = ShareTarget(media, current.value.source) },
                    )
                }
                if (media.isDemo) item { DemoBanner(Modifier.padding(horizontal = 20.dp)) }
                item(key = "summary") {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(media.title, style = MaterialTheme.typography.headlineLarge)
                        media.originalTitle
                            ?.takeIf { it.isNotBlank() && it != media.title }
                            ?.let {
                                Text(it, color = Muted, style = MaterialTheme.typography.bodyMedium)
                            }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            MetadataPill(media.key.type.label, highlighted = true)
                            media.year?.let { MetadataPill(it.toString()) }
                            details.runtimeMinutes?.let { MetadataPill("$it min") }
                            media.rating?.let {
                                MetadataPill("TMDB ${String.format(Locale.ROOT, "%.1f", it)} / 10")
                            }
                        }
                        if (details.genres.isNotEmpty())
                            Text(
                                details.genres.joinToString(" · ") { it.name },
                                color = Muted,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        Text(
                            media.overview ?: "Informação não disponível",
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD4D0DA),
                        )
                        Button(
                            onClick = { onPlay(media.key, null, null) },
                            enabled = current.value.source != null,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (current.value.source == null) "Reprodução indisponível"
                                else if (media.key.type == MediaType.TV) "Abrir player da série"
                                else "Abrir player"
                            )
                        }
                        Text(
                            if (media.isDemo)
                                "Os títulos de demonstração não têm fonte de reprodução."
                            else if (current.value.source == null)
                                "Não foi possível encontrar uma fonte de reprodução para este conteúdo."
                            else
                                "Iframe validado, disponibilidade a confirmar pelo fornecedor. ${if (media.key.type == MediaType.TV) "Ou escolhe um episódio abaixo." else "Os controlos pertencem ao player externo."}",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            OutlinedButton(
                                onClick = { library.toggle(media, LibraryFlag.WATCHLIST) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Icon(
                                    if (entry?.watchlisted == true) Icons.Outlined.Check
                                    else Icons.Outlined.Add,
                                    null,
                                    Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (entry?.watchlisted == true) "Na minha lista"
                                    else "Minha lista"
                                )
                            }
                            FilledTonalIconToggleButton(
                                checked = entry?.favorite == true,
                                onCheckedChange = { library.toggle(media, LibraryFlag.FAVORITE) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    if (entry?.favorite == true) Icons.Outlined.Favorite
                                    else Icons.Outlined.FavoriteBorder,
                                    if (entry?.favorite == true) "Remover dos favoritos"
                                    else "Adicionar aos favoritos",
                                )
                            }
                            FilterChip(
                                selected = entry?.watched == true,
                                onClick = { library.toggle(media, LibraryFlag.WATCHED) },
                                label = {
                                    Text(if (entry?.watched == true) "Visto" else "Marcar visto")
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.CheckCircleOutline,
                                        null,
                                        Modifier.size(18.dp),
                                    )
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }
                stickyHeader(key = "detail-tabs") {
                    TabRow(
                        selectedTabIndex = panels.indexOf(panel).coerceAtLeast(0),
                        containerColor = Ink,
                        contentColor = Accent,
                    ) {
                        panels.forEach { name ->
                            Tab(
                                selected = panel == name,
                                onClick = { panel = name },
                                text = { Text(name) },
                                modifier = Modifier.heightIn(min = 52.dp),
                            )
                        }
                    }
                }
                when (panel) {
                    "Episódios" -> {
                        item(key = "seasons") {
                            Column(
                                Modifier.padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SeasonSelector(details.seasons, selectedSeason) {
                                    seasonPicker = true
                                }
                                if (details.seasons.isEmpty())
                                    Text("Temporadas: Informação não disponível", color = Muted)
                                if (selectedSeason == 0)
                                    Text(
                                        "A reprodução de especiais não está documentada pelo fornecedor. Os metadados continuam disponíveis.",
                                        color = Muted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                Text(
                                    "Marca os episódios como vistos manualmente. Não é medido o progresso do iframe.",
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        when (val episodeState = episodes) {
                            LoadState.Loading ->
                                items(2) {
                                    Skeleton(
                                        Modifier.fillMaxWidth()
                                            .padding(horizontal = 20.dp)
                                            .height(190.dp)
                                    )
                                }
                            is LoadState.Error ->
                                item { ErrorPanel(episodeState.error, viewModel::retrySeason) }
                            is LoadState.Data -> {
                                if (episodeState.value.isEmpty())
                                    item {
                                        StatusPanel(
                                            "Ainda sem episódios",
                                            "A TMDB não devolveu episódios para esta temporada.",
                                        )
                                    }
                                items(episodeState.value, key = { "episode-${it.episode.id}" }) {
                                    item ->
                                    val watched =
                                        episodeWatches.any {
                                            it.season == item.episode.season &&
                                                it.episode == item.episode.number
                                        }
                                    EpisodeCard(
                                        item = item,
                                        watched = watched,
                                        onPlay = {
                                            onPlay(
                                                media.key,
                                                item.episode.season,
                                                item.episode.number,
                                            )
                                        },
                                        onWatched = {
                                            library.markEpisode(
                                                media.key,
                                                item.episode.season,
                                                item.episode.number,
                                                !watched,
                                            )
                                        },
                                        onShare = { shareTarget = ShareTarget(media, item.source) },
                                        modifier = Modifier.padding(horizontal = 20.dp),
                                    )
                                }
                            }
                            null -> Unit
                        }
                    }
                    "Sobre" -> {
                        item {
                            Column(
                                Modifier.padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                Text("A história", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    media.overview ?: "Informação não disponível",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFFD4D0DA),
                                )
                                if (details.trailer != null)
                                    OutlinedButton(
                                        onClick = { trailer = details.trailer },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.OpenInNew,
                                            null,
                                            Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Trailer oficial")
                                    }
                                else
                                    Text(
                                        "Trailer oficial: Informação não disponível",
                                        color = Muted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Ficha do título", style = MaterialTheme.typography.titleLarge)
                                MetadataLine("Título original", media.originalTitle)
                                MetadataLine("Data de lançamento", media.releaseDate)
                                MetadataLine(
                                    "Avaliação TMDB",
                                    media.rating?.let {
                                        String.format(Locale.ROOT, "%.1f / 10", it)
                                    },
                                )
                                MetadataLine(
                                    "Géneros",
                                    details.genres.joinToString(", ") { it.name }.ifBlank { null },
                                )
                                MetadataLine(
                                    "Duração",
                                    details.runtimeMinutes?.let { "$it minutos" },
                                )
                                MetadataLine(
                                    "País de origem",
                                    details.countries.joinToString(", ").ifBlank { null },
                                )
                                MetadataLine(
                                    "Realização",
                                    details.directors
                                        .joinToString(", ") { it.name }
                                        .ifBlank { null },
                                )
                                if (media.key.type == MediaType.TV)
                                    MetadataLine(
                                        "Criação",
                                        details.creators
                                            .joinToString(", ") { it.name }
                                            .ifBlank { null },
                                    )
                                MetadataLine(
                                    "TMDB",
                                    if (media.isDemo) null else media.key.id.toString(),
                                )
                                MetadataLine("IMDb", details.imdbId)
                            }
                        }
                        if (details.cast.isNotEmpty())
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        "Elenco",
                                        Modifier.padding(horizontal = 20.dp),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
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
                                )
                            }
                        if (details.directors.isNotEmpty() || details.creators.isNotEmpty())
                            item {
                                Column(
                                    Modifier.padding(horizontal = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        "Por detrás da história",
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                    (details.directors + details.creators)
                                        .distinctBy { it.id }
                                        .forEach { person ->
                                            TextButton(
                                                onClick = { onPerson(person.id) },
                                                modifier = Modifier.heightIn(min = 48.dp),
                                            ) {
                                                Text(person.name)
                                                Spacer(Modifier.width(8.dp))
                                                Icon(
                                                    Icons.AutoMirrored.Outlined.ArrowForward,
                                                    null,
                                                    Modifier.size(18.dp),
                                                )
                                            }
                                        }
                                }
                            }
                    }
                    "Mais títulos" -> {
                        if (details.recommendations.isNotEmpty())
                            item {
                                MediaRow(
                                    "Recomendações da TMDB",
                                    details.recommendations,
                                    onDetails,
                                )
                            }
                        if (details.similar.isNotEmpty())
                            item {
                                MediaRow(
                                    if (media.isDemo) "Outros exemplos fictícios"
                                    else "Na mesma atmosfera",
                                    details.similar,
                                    onDetails,
                                )
                            }
                        if (details.recommendations.isEmpty() && details.similar.isEmpty())
                            item {
                                StatusPanel(
                                    "Sem recomendações",
                                    "Não há informação disponível para este título.",
                                )
                            }
                    }
                }
            }
            if (seasonPicker)
                SeasonPickerSheet(details.seasons, selectedSeason, viewModel::selectSeason) {
                    seasonPicker = false
                }
        }
    }
    shareTarget?.let { target -> MediaShareSheet(target, policy) { shareTarget = null } }
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
private fun DetailHero(media: MediaSummary, onBack: () -> Unit, onShare: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(310.dp)) {
        MediaArtwork(media, Modifier.fillMaxSize(), backdrop = true)
        Box(
            Modifier.fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Ink.copy(alpha = 0.25f), Color.Transparent, Ink))
                )
        )
        Row(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FilledIconButton(
                onClick = onBack,
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = Ink.copy(alpha = 0.7f)
                    ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar")
            }
            FilledIconButton(
                onClick = onShare,
                enabled = !media.isDemo,
                colors =
                    IconButtonDefaults.filledIconButtonColors(
                        containerColor = Ink.copy(alpha = 0.7f)
                    ),
                modifier = Modifier.size(48.dp),
            ) {
                Icon(Icons.Outlined.Share, "Partilhar título ou link MyEmbed")
            }
        }
        Row(
            Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MediaArtwork(
                media,
                Modifier.width(96.dp).height(144.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(
                Modifier.padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "MIREVIA / DESCOBRIR",
                    color = Accent,
                    style = MaterialTheme.typography.labelSmall,
                )
                MetadataPill(if (media.isDemo) "Demonstração" else "METADADOS TMDB")
            }
        }
    }
}

@Composable
private fun MetadataLine(label: String, value: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            label.uppercase(Locale.ROOT),
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(value ?: "Informação não disponível", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PersonCard(person: PersonSummary, onClick: () -> Unit) {
    Column(
        Modifier.width(108.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(108.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (person.profileUrl != null) MediaStill(person.profileUrl, Modifier.fillMaxSize())
            else
                Icon(
                    Icons.Outlined.PersonOutline,
                    "Retrato não disponível",
                    Modifier.size(32.dp),
                    tint = Muted,
                )
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
