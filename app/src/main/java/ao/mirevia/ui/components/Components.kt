package ao.mirevia.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ao.mirevia.R
import ao.mirevia.core.model.*
import ao.mirevia.ui.theme.*
import coil3.compose.SubcomposeAsyncImage
import java.net.URI
import java.util.Locale

@Composable
fun Brand(modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_mirevia),
            null,
            Modifier.size(34.dp),
            tint = Color.Unspecified,
        )
        Text(
            "MIREVIA",
            style = MaterialTheme.typography.titleLarge,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null)
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            if (subtitle != null)
                Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        actions()
    }
}

@Composable
fun DemoBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Science, null, Modifier.size(20.dp), tint = Mint)
            Column {
                Text(
                    "Modo de demonstração",
                    color = Mint,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    "Títulos fictícios. Configura a TMDB para obter dados reais. A reprodução está desactivada.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
fun StatusPanel(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Movie,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(icon, null, Modifier.padding(18.dp).size(28.dp), tint = Mint)
        }
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = Muted)
        if (action != null && onAction != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}

@Composable
fun ErrorPanel(error: UiError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val offline = error.offline && !LocalNetworkAvailable.current
    StatusPanel(
        if (offline) "Sem ligação à Internet" else error.title,
        if (offline)
            "Liga-te à Internet e tenta novamente. A tua biblioteca continua guardada neste dispositivo."
        else error.message,
        modifier,
        if (offline) Icons.Outlined.WifiOff else Icons.Outlined.ErrorOutline,
        "Tentar novamente",
        onRetry,
    )
}

@Composable
fun Skeleton(modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val alpha =
        if (reduceMotion) 0.65f
        else {
            val transition = rememberInfiniteTransition(label = "Carregamento")
            val value by
                transition.animateFloat(
                    0.35f,
                    0.8f,
                    infiniteRepeatable(tween(850), RepeatMode.Reverse),
                    label = "Skeleton",
                )
            value
        }
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

@Composable
fun CatalogSkeleton() {
    Column(
        Modifier.padding(20.dp).semantics { contentDescription = "A carregar conteúdos" },
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Skeleton(Modifier.fillMaxWidth().height(340.dp))
        Skeleton(Modifier.width(190.dp).height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { Skeleton(Modifier.weight(1f).height(190.dp)) }
        }
    }
}

private fun allowedImageUrl(value: String?): String? =
    value?.takeIf {
        runCatching {
                URI(it).let { uri ->
                    uri.scheme == "https" &&
                        uri.host == "image.tmdb.org" &&
                        uri.rawUserInfo == null &&
                        uri.port == -1
                }
            }
            .getOrDefault(false)
    }

@Composable
fun MediaArtwork(media: MediaSummary, modifier: Modifier = Modifier, backdrop: Boolean = false) {
    val url =
        allowedImageUrl(if (backdrop) media.backdropUrl ?: media.posterUrl else media.posterUrl)
    if (url == null) AbstractArtwork(media, modifier)
    else
        SubcomposeAsyncImage(
            model = url,
            contentDescription = if (backdrop) null else "Poster de ${media.title}",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = { Skeleton(Modifier.fillMaxSize()) },
            error = { AbstractArtwork(media, Modifier.fillMaxSize()) },
        )
}

@Composable
private fun AbstractArtwork(media: MediaSummary, modifier: Modifier) {
    val palettes =
        listOf(
            listOf(Color(0xFF284D52), Color(0xFF12232D), Color(0xFF071017)),
            listOf(Color(0xFF453764), Color(0xFF20233D), Color(0xFF10131E)),
            listOf(Color(0xFF72534B), Color(0xFF382C38), Color(0xFF171822)),
            listOf(Color(0xFF57614D), Color(0xFF1F3C3B), Color(0xFF0A2028)),
        )
    val colors = palettes[(media.key.id - 1) % palettes.size]
    Box(modifier.background(Brush.verticalGradient(colors))) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.62f, size.height * 0.36f)
            drawCircle(Color.White.copy(alpha = 0.06f), size.width * 0.66f, center)
            drawCircle(Color.White.copy(alpha = 0.07f), size.width * 0.43f, center)
            drawCircle(Color.White.copy(alpha = 0.11f), size.width * 0.23f, center)
            drawLine(
                Color.White.copy(alpha = 0.12f),
                Offset(-size.width, size.height * 0.8f),
                Offset(size.width * 2, size.height * 0.4f),
                2f,
            )
        }
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            if (media.isDemo)
                Text(
                    "MIREVIA / DEMO",
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.7f),
                )
            else Icon(Icons.Outlined.Movie, null, tint = Color.White.copy(alpha = 0.6f))
            if (media.isDemo)
                Text(
                    media.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 23.sp,
                    color = Color.White,
                )
        }
    }
}

@Composable
fun MediaCard(media: MediaSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(142.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp)
    ) {
        Box {
            MediaArtwork(
                media,
                Modifier.fillMaxWidth().aspectRatio(2f / 3).clip(RoundedCornerShape(12.dp)),
            )
            if (media.rating != null)
                Surface(
                    Modifier.align(Alignment.TopEnd).padding(7.dp),
                    color = Ink.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(7.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Star,
                            null,
                            Modifier.size(12.dp),
                            tint = Color(0xFFDFC58F),
                        )
                        Text(
                            String.format(Locale.ROOT, "%.1f", media.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                        )
                    }
                }
        }
        Text(
            media.title,
            Modifier.padding(top = 10.dp, start = 2.dp, end = 2.dp),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (media.isDemo) "Demonstração"
            else listOfNotNull(media.year?.toString(), media.key.type.label).joinToString(" · "),
            Modifier.padding(start = 2.dp, top = 3.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Muted,
        )
    }
}

@Composable
fun MediaRow(
    title: String,
    media: List<MediaSummary>,
    onDetails: (MediaKey) -> Unit,
    onSeeAll: (() -> Unit)? = null,
) {
    if (media.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            if (onSeeAll != null)
                IconButton(onClick = onSeeAll) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        "Ver todos: $title",
                        tint = Muted,
                    )
                }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(media, key = { it.key.storageKey }) { item ->
                MediaCard(item, { onDetails(item.key) })
            }
        }
    }
}

@Composable
fun <T> ChoiceMenu(
    label: String,
    selected: T,
    choices: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text("$label: ${choices.find { it.first == selected }?.second ?: "Todos"}")
            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choices.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}
