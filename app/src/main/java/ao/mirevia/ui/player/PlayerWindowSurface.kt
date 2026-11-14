package ao.mirevia.ui.player

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ao.mirevia.core.playback.PlaybackBlock
import ao.mirevia.playback.PlayerWindowController
import ao.mirevia.ui.components.StatusPanel
import ao.mirevia.ui.theme.Ink
import ao.mirevia.ui.theme.Muted

/** Separate native chrome makes third-party windows visibly distinct from the authorised iframe. */
@Composable
fun PlayerWindowSurface(
    origin: PlaybackBlock?,
    loading: Boolean,
    failure: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(modifier.fillMaxSize(), color = Ink) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp).heightIn(min = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    Text(
                        "Janela do player · terceiros",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val label =
                        if (origin?.scheme == "https" && origin.host != null) {
                            "Origem: https://${origin.host}${origin.port?.let { ":$it" }.orEmpty()}"
                        } else "Endereço ainda não disponível"
                    Text(
                        label,
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.Close, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Fechar")
                }
            }
            if (loading && failure == null)
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().semantics {
                        contentDescription = "A carregar a página da janela"
                    }
                )
            Box(Modifier.fillMaxWidth().weight(1f).background(Ink)) {
                if (failure != null)
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        StatusPanel(
                            "Não foi possível abrir esta janela",
                            failure,
                            icon = Icons.Outlined.Security,
                        )
                        Text(
                            "Podes fechá-la para regressar ao iframe. A Mirevia não muda os anúncios ou os controlos do fornecedor.",
                            Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Muted,
                        )
                    }
                else content()
            }
        }
    }
}

/**
 * The same overlay is used by the player and the transport tests; windows own their native views.
 */
@Composable
internal fun PlayerWindowsOverlay(
    windows: PlayerWindowController,
    snackbar: SnackbarHostState? = null,
) {
    val localSnackbar = remember { SnackbarHostState() }
    windows.activeWindow?.let { entry ->
        val page = entry.value
        // Same Activity, a distinct native modal window: keeps TalkBack/focus out of the covered
        // iframe without hiding or destroying its document. This does not launch another app.
        Dialog(
            onDismissRequest = { windows.close(entry.id) },
            properties =
                DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false,
                    decorFitsSystemWindows = false,
                ),
        ) {
            Box(Modifier.fillMaxSize()) {
                PlayerWindowSurface(
                    page.origin,
                    page.loading,
                    page.failure,
                    { windows.close(entry.id) },
                ) {
                    key(entry.id) {
                        AndroidView(
                            factory = {
                                page.browser.also { browser ->
                                    (browser.parent as? ViewGroup)?.removeView(browser)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            // Keep an opener alive while its child is on top. Only the controller
                            // destroys windows; detaching a holder is not the end of the window.
                            onRelease = {},
                        )
                    }
                }
                SnackbarHost(
                    snackbar ?: localSnackbar,
                    Modifier.align(Alignment.BottomCenter).safeDrawingPadding(),
                )
            }
        }
    }
}
