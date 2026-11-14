package ao.mirevia.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ao.mirevia.BuildConfig
import ao.mirevia.core.model.SharePayload
import ao.mirevia.security.AppIdentity
import ao.mirevia.security.readAppIdentity
import ao.mirevia.ui.components.copyPublicText
import ao.mirevia.ui.components.openTrustedExternal
import ao.mirevia.ui.theme.Accent
import ao.mirevia.ui.theme.Muted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSecuritySheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var identity by remember { mutableStateOf<AppIdentity?>(null) }
    var loading by remember { mutableStateOf(true) }
    var notice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        identity = withContext(Dispatchers.IO) { readAppIdentity(context.applicationContext) }
        loading = false
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
            Text("TRANSPARÊNCIA", color = Accent, style = MaterialTheme.typography.labelSmall)
            Text("Segurança e assinatura", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Se o Google Play Protect apresentar um alerta, interrompe a utilização e verifica o aviso. Não desactives a protecção. Sem o texto completo e o APK analisado, não é possível confirmar a causa ou afirmar que é um falso positivo.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (BuildConfig.DEBUG)
                Text(
                    "Este é um APK debug pessoal, não uma publicação validada pela Google Play. Uma assinatura válida não é uma certificação de segurança.",
                    color = Muted,
                )
            else
                Text(
                    "A identidade da assinatura não substitui a análise de segurança da aplicação.",
                    color = Muted,
                )
            if (loading) CircularProgressIndicator(Modifier.size(24.dp))
            else
                identity?.let { info ->
                    SelectionContainer {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                info.packageName,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text("Versão ${info.versionName}", color = Muted)
                            Text(
                                "Certificados SHA-256",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            info.signerSha256.forEach { fingerprint ->
                                Text(
                                    fingerprint,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (info.signerSha256.isEmpty())
                                Text("Informação não disponível", color = Muted)
                            Text(
                                "Não é o hash do ficheiro APK. Compara estes fingerprints com os de uma build de origem conhecida.",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            HorizontalDivider()
                            Text(
                                "Permissões do pacote instalado",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            info.requestedPermissions.forEach { permission ->
                                Text(
                                    permission,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                "O manifesto da Mirevia pede Internet e estado da rede. Bibliotecas Android podem acrescentar permissões internas; a lista acima é a do pacote instalado, não uma garantia de segurança.",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            notice =
                                if (
                                    copyPublicText(
                                        context,
                                        SharePayload("Identidade da Mirevia", info.publicReport()),
                                    )
                                )
                                    "Identificação copiada. Não inclui tokens nem chaves privadas."
                                else "Não foi possível copiar."
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Copiar identificação")
                    }
                } ?: Text("Não foi possível ler a identidade do pacote.", color = Muted)
            Text(
                "O player não recebe câmara, microfone, localização ou ficheiros. Safe Browsing e verificações TLS permanecem activos. Isso reduz riscos, mas o iframe é um serviço externo e não pode ser declarado seguro por esta revisão.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "O APK pessoal pode conter um token TMDB recuperável. Evita enviá-lo a scanners públicos sem considerar a exposição da credencial. Partilhar um link de título não partilha esse token.",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(
                onClick = {
                    if (
                        !openTrustedExternal(
                            context,
                            "https://support.google.com/googleplay/answer/2812853?hl=pt",
                            setOf("support.google.com"),
                        )
                    )
                        notice = "Não foi possível abrir a ajuda oficial."
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Ajuda oficial do Play Protect")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(18.dp))
            }
            notice?.let { Text(it, color = Accent, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(12.dp))
        }
    }
}
