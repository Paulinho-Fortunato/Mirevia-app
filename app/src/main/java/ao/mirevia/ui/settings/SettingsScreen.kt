package ao.mirevia.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.mirevia.BuildConfig
import ao.mirevia.R
import ao.mirevia.app.AppContainer
import ao.mirevia.data.local.UserPreferences
import ao.mirevia.ui.components.*
import ao.mirevia.ui.library.LibraryViewModel
import ao.mirevia.ui.theme.*
import coil3.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val TMDB_LOGO =
    "https://www.themoviedb.org/assets/v4/logos/v2/blue_long_2-9665a76b1ae401a510ec1e0ca40ddcb3b0cfe45f1d51b77a308fea0845885648.svg"

@Composable
fun SettingsScreen(container: AppContainer, library: LibraryViewModel, onLegal: (String) -> Unit) {
    val preferences by
        container.preferences.preferences.collectAsStateWithLifecycle(UserPreferences())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var clearDialog by remember { mutableStateOf<String?>(null) }
    var settingError by remember { mutableStateOf(false) }
    var securitySheet by remember { mutableStateOf(false) }
    var consentDialog by remember { mutableStateOf(false) }
    fun update(block: suspend () -> Unit) {
        coroutineScope.launch {
            try {
                block()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                settingError = true
            }
        }
    }
    LazyColumn(
        contentPadding = PaddingValues(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { ScreenHeader("Definições", "Sem contas. Sem subscrições. Só histórias.") }
        if (preferences.storageError)
            item {
                Text(
                    "Não foi possível ler as preferências locais. O player e o histórico ficam desactivados por segurança. Fecha a aplicação e tenta novamente.",
                    Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        item {
            Surface(
                Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Brand()
                    Text(
                        "O teu cinema, num lugar teu.",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        container.connectionLabel,
                        color = Accent,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Projecto pessoal e não comercial · versão ${BuildConfig.VERSION_NAME}",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item {
            Column {
                SectionLabel("A tua experiência")
                PreferenceSwitch(
                    "Reduzir animações",
                    "Skeletons estáticos e transições mais discretas.",
                    preferences.reduceMotion,
                ) {
                    update { container.preferences.setReduceMotion(it) }
                }
                PreferenceSwitch(
                    "Guardar histórico local",
                    "Registar apenas a abertura e saída do player. Não mede o tempo visto.",
                    preferences.saveHistory,
                ) {
                    update { container.preferences.setSaveHistory(it) }
                }
                PreferenceSwitch(
                    "Permitir navegador externo",
                    "Disponibiliza a página oficial de demonstração do fornecedor, que utiliza iframe. Nunca abre um stream directo.",
                    preferences.externalBrowser,
                ) {
                    update { container.preferences.setExternalBrowser(it) }
                }
                PreferenceSwitch(
                    "Consentimento do player externo",
                    "Permite recursos, frames e janelas HTTPS dentro da Mirevia, sem lista por domínio, incluindo métricas e publicidade. Requer confirmação renovada. Desactivar impede novos carregamentos.",
                    preferences.externalPlayerConsent,
                ) { enabled ->
                    if (enabled) consentDialog = true
                    else update { container.preferences.setPlayerConsent(false) }
                }
            }
        }
        item {
            Column {
                SectionLabel("Dados neste dispositivo")
                SettingsAction("Limpar histórico de reprodução", Icons.Outlined.History) {
                    clearDialog = "history"
                }
                SettingsAction("Limpar pesquisas recentes", Icons.Outlined.Search) {
                    clearDialog = "search"
                }
                SettingsAction("Apagar biblioteca e dados locais", Icons.Outlined.DeleteOutline) {
                    clearDialog = "all"
                }
                Text(
                    "Não existe sincronização na cloud. Desinstalar a aplicação remove os dados locais. O backup automático da aplicação está desactivado.",
                    Modifier.padding(horizontal = 20.dp),
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item {
            Column {
                SectionLabel("Privacidade e utilização")
                SettingsAction("Segurança e assinatura do APK", Icons.Outlined.Security) {
                    securitySheet = true
                }
                SettingsAction("Política de privacidade", Icons.Outlined.PrivacyTip) {
                    onLegal("privacy")
                }
                SettingsAction("Termos de utilização", Icons.Outlined.Description) {
                    onLegal("terms")
                }
            }
        }
        item {
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Sobre e créditos", style = MaterialTheme.typography.titleLarge)
                // Official approved asset, unchanged. Text attribution remains visible if the image
                // is offline.
                AsyncImage(
                    model = TMDB_LOGO,
                    contentDescription = "Logótipo da The Movie Database",
                    modifier = Modifier.width(140.dp).height(24.dp),
                    contentScale = ContentScale.Fit,
                )
                Text(
                    stringResource(R.string.tmdb_attribution),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "A TMDB fornece metadados e imagens, não direitos de transmissão nem ficheiros de vídeo. A Mirevia não é afiliada, certificada, patrocinada ou aprovada pela TMDB.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(
                    onClick = {
                        if (
                            !openTrustedExternal(
                                context,
                                "https://www.themoviedb.org",
                                setOf("www.themoviedb.org"),
                            )
                        )
                            settingError = true
                    }
                ) {
                    Text("Visitar a TMDB")
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        null,
                        Modifier.padding(start = 8.dp).size(16.dp),
                    )
                }
                Text(
                    "Fornecedor externo: EmbedMovies",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.external_player_notice),
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "O player mantém os seus controlos e eventuais anúncios. A aplicação não extrai vídeos, não oferece downloads e não contorna restrições. Disponibilidade, PiP no próprio iframe e funcionalidades dependem do fornecedor.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Retoma exacta e progresso automático: não disponíveis sem eventos oficiais do fornecedor. Nenhum progresso é simulado.",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
    clearDialog?.let { kind ->
        AlertDialog(
            onDismissRequest = { clearDialog = null },
            title = { Text("Apagar dados locais?") },
            text = {
                Text(
                    when (kind) {
                        "history" -> "O histórico de players abertos será apagado."
                        "search" -> "As pesquisas recentes serão apagadas."
                        else ->
                            "Favoritos, lista, vistos, episódios vistos, histórico e pesquisas serão apagados. Não é possível anular esta acção."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (kind) {
                            "history" -> library.clearHistory()
                            "search" -> library.clearSearches()
                            else -> library.clearAll()
                        }
                        clearDialog = null
                    }
                ) {
                    Text("Apagar")
                }
            },
            dismissButton = { TextButton(onClick = { clearDialog = null }) { Text("Cancelar") } },
        )
    }
    if (consentDialog)
        ExternalPlayerConsentDialog(
            onAccept = {
                update {
                    container.preferences.setPlayerConsent(true)
                    consentDialog = false
                }
            },
            onDismiss = { consentDialog = false },
            storageError = preferences.storageError,
        )
    if (securitySheet) AppSecuritySheet { securitySheet = false }
    if (settingError)
        AlertDialog(
            onDismissRequest = { settingError = false },
            title = { Text("Não foi possível concluir") },
            text = {
                Text(
                    "Não foi possível guardar a preferência ou abrir o navegador. Tenta novamente."
                )
            },
            confirmButton = { TextButton(onClick = { settingError = false }) { Text("Fechar") } },
        )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        color = Accent,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun PreferenceSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(description, color = Muted) },
        trailingContent = {
            Switch(
                checked,
                onCheckedChange = onChange,
                modifier = Modifier.semantics { contentDescription = title },
            )
        },
        colors = ListItemDefaults.colors(containerColor = Ink),
    )
}

@Composable
private fun SettingsAction(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Icon(icon, null, tint = Muted)
        Text(
            title,
            Modifier.weight(1f).padding(horizontal = 14.dp),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = Muted)
    }
}
