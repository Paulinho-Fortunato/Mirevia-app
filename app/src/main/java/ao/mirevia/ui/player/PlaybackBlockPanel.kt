package ao.mirevia.ui.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ao.mirevia.core.playback.EmbedUrlPolicy
import ao.mirevia.core.playback.IframeRequestPolicy
import ao.mirevia.core.playback.PlaybackBlock
import ao.mirevia.core.playback.PlaybackBlockKind
import ao.mirevia.core.playback.PlayerWindowPolicy
import ao.mirevia.ui.components.StatusPanel
import ao.mirevia.ui.theme.Accent
import ao.mirevia.ui.theme.Muted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaybackBlockPanel(
    block: PlaybackBlock?,
    policy: EmbedUrlPolicy,
    onRetry: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message =
        when (block?.kind) {
            PlaybackBlockKind.RESOURCE ->
                "O pedido não cumpre a política de recursos do iframe. São permitidos domínios remotos HTTPS na porta 443, mas não HTTP, credenciais no endereço, URLs malformados ou a origem reservada à página local."
            PlaybackBlockKind.NAVIGATION ->
                "Foi bloqueada uma navegação. A página principal tem de permanecer no wrapper local; os frames internos aceitam HTTPS na porta 443, sem credenciais no endereço."
            PlaybackBlockKind.NEW_WINDOW ->
                "Não foi possível abrir a janela pedida pelo player. Na política actual, as janelas HTTPS abrem dentro da Mirevia sem lista por domínio; criar uma janela não deve interromper o iframe principal."
            PlaybackBlockKind.DOWNLOAD ->
                "O player tentou iniciar um download. A Mirevia só permite reprodução por iframe, não ficheiros de vídeo."
            PlaybackBlockKind.UNSAFE_PAGE ->
                "O Android identificou um pedido inseguro. O Safe Browsing foi mantido activo e a navegação foi cancelada."
            PlaybackBlockKind.WEBVIEW_FEATURE ->
                "Actualiza o Android System WebView. Esta versão não permite aplicar a política de segurança aos service workers."
            null ->
                "O player foi interrompido por segurança. Não foi identificada uma origem verificável."
        }
    Column(modifier.verticalScroll(rememberScrollState())) {
        StatusPanel("Player interrompido", message, icon = Icons.Outlined.Security)
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "DIAGNÓSTICO LOCAL",
                    color = Accent,
                    style = MaterialTheme.typography.labelSmall,
                )
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "Domínio observado: ${block?.host ?: "não identificado"}",
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Esquema: ${block?.scheme ?: "não identificado"}",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        block?.port?.let { port ->
                            Text(
                                "Porta explícita: $port",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            "Iframe inicial autorizado: ${policy.allowedHosts.sorted().joinToString(", ")}",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    "Só se mostra a origem, nunca o caminho, parâmetros, cookies ou tokens. Copiar ou partilhar é opcional; nada é enviado automaticamente.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    IframeRequestPolicy.RESOURCE_DESCRIPTION,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    PlayerWindowPolicy.DESCRIPTION,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Um hostname não listado já não causa bloqueio por si só. HTTPS não certifica a origem como segura; o Safe Browsing e as outras protecções podem interromper a sessão.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onCopy,
                        enabled = block != null,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Copiar diagnóstico")
                    }
                    TextButton(
                        onClick = onShare,
                        enabled = block != null,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Outlined.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Partilhar diagnóstico")
                    }
                    OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text("Tentar novamente")
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
