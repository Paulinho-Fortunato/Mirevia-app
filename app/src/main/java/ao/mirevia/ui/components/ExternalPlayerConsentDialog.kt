package ao.mirevia.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ao.mirevia.R
import ao.mirevia.core.playback.IframeRequestPolicy

/**
 * Shared by player and settings: enabling the wider policy always requires explicit confirmation.
 */
@Composable
fun ExternalPlayerConsentDialog(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    storageError: Boolean = false,
    storageNotice: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permitir recursos HTTPS externos?") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.external_player_notice))
                Text(
                    "O iframe começa em myembed.biz. O documento do fornecedor pode carregar recursos e frames HTTPS de outros domínios, incluindo CDNs, publicidade e serviços de métricas como Cloudflare Insights. O fornecedor e esses terceiros podem receber o teu IP e dados técnicos/navegação."
                )
                Text(IframeRequestPolicy.RESOURCE_DESCRIPTION)
                Text(
                    "Não será pedida aprovação para cada domínio. HTTPS não garante que uma origem seja de confiança. A Mirevia não injecta scripts, não altera os anúncios ou controlos e não mede nem simula o progresso do vídeo."
                )
                Text(
                    "TLS e Safe Browsing mantêm-se activos quando suportados. Downloads, ficheiros, permissões sensíveis e janelas externas não verificadas continuam bloqueados. Podes revogar esta autorização nas definições."
                )
                if (storageError)
                    Text(
                        "Não foi possível ler as preferências. Fecha a aplicação e tenta novamente. O iframe não será carregado.",
                        color = MaterialTheme.colorScheme.error,
                    )
                storageNotice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onAccept,
                enabled = !storageError,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Aceitar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Cancelar")
            }
        },
    )
}
