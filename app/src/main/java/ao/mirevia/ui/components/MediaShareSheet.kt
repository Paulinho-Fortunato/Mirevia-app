package ao.mirevia.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ao.mirevia.core.model.*
import ao.mirevia.core.playback.EmbedUrlPolicy
import ao.mirevia.core.playback.VideoSource
import ao.mirevia.ui.theme.Accent
import ao.mirevia.ui.theme.Muted

/** The source is resolved and validated, never a resource URL discovered inside the iframe. */
data class ShareTarget(val media: MediaSummary, val source: VideoSource? = null)

fun publicShareIntent(payload: SharePayload): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, payload.subject)
        putExtra(Intent.EXTRA_TEXT, payload.text)
    }

fun sharePublicText(context: Context, payload: SharePayload): Boolean =
    try {
        context.startActivity(Intent.createChooser(publicShareIntent(payload), "Partilhar com"))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

fun copyPublicText(context: Context, payload: SharePayload): Boolean =
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) false
        else {
            clipboard.setPrimaryClip(ClipData.newPlainText(payload.subject, payload.text))
            true
        }
    } catch (_: SecurityException) {
        false
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaShareSheet(target: ShareTarget, policy: EmbedUrlPolicy, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val title = remember(target) { MediaSharing.title(target.media) }
    val embed = remember(target, policy) { MediaSharing.embed(target.media, target.source, policy) }
    var shareError by remember { mutableStateOf(false) }
    fun send(payload: SharePayload?) {
        if (payload != null && sharePublicText(context, payload)) onDismiss() else shareError = true
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "PARTILHAR UMA HISTÓRIA",
                color = Accent,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(target.media.title, style = MaterialTheme.typography.headlineMedium)
            Text(
                "Escolhe o que enviar. Nada é partilhado sem seleccionares uma aplicação.",
                color = Muted,
                style = MaterialTheme.typography.bodyMedium,
            )
            ShareOption(
                title = "Partilhar título",
                description = "Página do filme ou da série na TMDB. Não é reprodução.",
                icon = Icons.Outlined.Movie,
                enabled = title != null,
                onClick = { send(title) },
            )
            ShareOption(
                title = "Partilhar link MyEmbed",
                description =
                    if (embed != null) embed.subject
                    else "Não há um link de incorporação válido para esta selecção.",
                icon = Icons.Outlined.Link,
                enabled = embed != null,
                onClick = { send(embed) },
            )
            Text(
                "O link MyEmbed destina-se exclusivamente a um iframe. Não é um ficheiro de vídeo e pode não funcionar quando aberto directamente noutra app. A disponibilidade e a privacidade dependem do fornecedor e da aplicação destinatária.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (target.media.isDemo)
                Text("Os títulos fictícios não têm links reais.", color = Muted)
            if (shareError)
                Text(
                    "Não foi possível abrir a folha de partilha.",
                    color = MaterialTheme.colorScheme.error,
                )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareOption(
    title: String,
    description: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = enabled, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.75f else 0.3f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier.heightIn(min = 80.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(icon, null, tint = if (enabled) Accent else Muted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else Muted,
                )
                Text(description, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}
