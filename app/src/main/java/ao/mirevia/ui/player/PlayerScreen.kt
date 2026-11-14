package ao.mirevia.ui.player

import android.app.Activity
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.mirevia.R
import ao.mirevia.app.AppContainer
import ao.mirevia.core.model.SharePayload
import ao.mirevia.data.local.UserPreferences
import ao.mirevia.playback.*
import ao.mirevia.ui.components.*
import ao.mirevia.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    container: AppContainer,
    onExit: () -> Unit,
    onImmersive: (Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val preferences: UserPreferences? by
        container.preferences.preferences.collectAsStateWithLifecycle(initialValue = null)
    val online by container.network.isOnline.collectAsStateWithLifecycle(initialValue = true)
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val fullscreen = remember { EmbedFullscreenController() }
    var expanded by remember { mutableStateOf(false) }
    var activeHost by remember { mutableStateOf<SecureEmbedHost?>(null) }
    var sessionEpoch by remember { mutableIntStateOf(0) }
    var foreground by remember {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var externalDialog by remember { mutableStateOf(false) }
    var externalError by remember { mutableStateOf(false) }
    var shareTarget by remember { mutableStateOf<ShareTarget?>(null) }
    var information by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun feedback(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }
    val windows = remember { PlayerWindowController { message -> feedback(message) } }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> foreground = true
                Lifecycle.Event.ON_STOP -> {
                    // Destruction must happen here, not only in a later (possibly paused)
                    // recomposition.
                    foreground = false
                    sessionEpoch++
                    activeHost?.release()
                    activeHost = null
                    windows.closeAll()
                    fullscreen.hide()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            activeHost?.release()
            activeHost = null
            windows.closeAll()
        }
    }
    val embedVisible =
        state.phase == PlayerPhase.AVAILABLE &&
            preferences?.externalPlayerConsent == true &&
            foreground
    DisposableEffect(embedVisible) {
        val window = (context as? Activity)?.window
        if (embedVisible) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    LaunchedEffect(expanded, fullscreen.customView) {
        onImmersive(expanded || fullscreen.customView != null)
    }
    DisposableEffect(Unit) {
        onDispose {
            fullscreen.hide()
            onImmersive(false)
        }
    }
    BackHandler {
        when {
            windows.activeWindow != null -> windows.closeActive()
            fullscreen.customView != null -> fullscreen.hide()
            expanded -> expanded = false
            else -> onExit()
        }
    }
    LaunchedEffect(
        state.attempt,
        state.source?.url,
        sessionEpoch,
        foreground,
        preferences?.externalPlayerConsent,
    ) {
        if (embedVisible) {
            delay(30_000)
            if (
                lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) &&
                    viewModel.state.value.phase == PlayerPhase.AVAILABLE &&
                    viewModel.state.value.wrapperLoading
            )
                viewModel.failed()
        }
    }
    MireviaTheme(preferences?.reduceMotion ?: false, networkAvailable = online) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
            val compact = maxHeight < 440.dp
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (!expanded)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onExit) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar")
                        }
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text(
                                state.details?.media?.title ?: "Reprodução",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                viewModel.episodeLabel ?: "EMBEDMOVIES · IFRAME EXTERNO",
                                color = Muted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        state.details
                            ?.media
                            ?.takeUnless { it.isDemo }
                            ?.let { media ->
                                IconButton(
                                    onClick = { shareTarget = ShareTarget(media, state.source) }
                                ) {
                                    Icon(Icons.Outlined.Share, "Partilhar título ou link MyEmbed")
                                }
                            }
                        if (compact)
                            IconButton(onClick = { information = true }) {
                                Icon(Icons.Outlined.Info, "Informação do player externo")
                            }
                        if (state.source?.supportsFullscreen == true)
                            IconButton(onClick = { expanded = true }) {
                                Icon(Icons.Outlined.Fullscreen, "Expandir ecrã do player")
                            }
                    }
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    when (state.phase) {
                        PlayerPhase.RESOLVING ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    "A obter metadados e a validar o iframe…",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        PlayerPhase.UNAVAILABLE ->
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                StatusPanel(
                                    "Conteúdo indisponível",
                                    stringResource(R.string.player_unavailable),
                                    action = "Tentar novamente",
                                    onAction = viewModel::retry,
                                )
                            }
                        PlayerPhase.ERROR ->
                            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                                StatusPanel(
                                    "Não foi possível carregar",
                                    stringResource(R.string.player_load_error),
                                    icon = Icons.Outlined.ErrorOutline,
                                    action = "Repetir carregamento",
                                    onAction = viewModel::retry,
                                )
                                state.reason?.let {
                                    Text(
                                        it,
                                        Modifier.padding(horizontal = 28.dp),
                                        color = Muted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        PlayerPhase.BLOCKED ->
                            PlaybackBlockPanel(
                                block = state.block,
                                policy = container.embedPolicy,
                                onRetry = viewModel::retry,
                                onCopy = {
                                    state.block?.let {
                                        val copied =
                                            copyPublicText(
                                                context,
                                                SharePayload(
                                                    "Diagnóstico do iframe",
                                                    it.report(container.embedPolicy),
                                                ),
                                            )
                                        feedback(
                                            if (copied)
                                                "Diagnóstico copiado, sem URLs de recursos ou tokens."
                                            else "Não foi possível copiar."
                                        )
                                    }
                                },
                                onShare = {
                                    state.block?.let {
                                        if (
                                            !sharePublicText(
                                                context,
                                                SharePayload(
                                                    "Diagnóstico do iframe",
                                                    it.report(container.embedPolicy),
                                                ),
                                            )
                                        )
                                            feedback("Não foi possível abrir a folha de partilha.")
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        PlayerPhase.AVAILABLE -> {
                            val source = state.source
                            when {
                                preferences == null -> CircularProgressIndicator()
                                preferences?.externalPlayerConsent != true ->
                                    Text(
                                        "É necessário consentimento antes de contactar o player externo.",
                                        Modifier.padding(24.dp),
                                    )
                                !foreground ->
                                    Text(
                                        "O player será recarregado ao regressares. Não existe reprodução em segundo plano.",
                                        Modifier.padding(24.dp),
                                    )
                                source != null && container.embedPolicy.isValidSource(source) ->
                                    key(source.url, state.attempt, sessionEpoch) {
                                        val attempt = state.attempt
                                        AndroidView(
                                            factory = { webContext ->
                                                // End the previous session before configuring the
                                                // process-wide worker gate or clearing shared data.
                                                activeHost?.release()
                                                SecureEmbedHost(
                                                        webContext,
                                                        source,
                                                        container.embedPolicy,
                                                        fullscreen,
                                                        windows,
                                                        viewModel::sessionOpened,
                                                        viewModel::sessionClosed,
                                                        { viewModel.wrapperLoaded(attempt) },
                                                        { viewModel.failAttempt(attempt) },
                                                        { reason ->
                                                            viewModel.blocked(attempt, reason)
                                                        },
                                                    )
                                                    .also { activeHost = it }
                                            },
                                            modifier = Modifier.fillMaxSize(),
                                            onRelease = { host ->
                                                if (activeHost === host) activeHost = null
                                                host.release()
                                            },
                                        )
                                        if (state.wrapperLoading)
                                            Surface(
                                                Modifier.align(Alignment.Center),
                                                color = Ink.copy(alpha = 0.9f),
                                                shape = MaterialTheme.shapes.medium,
                                            ) {
                                                Row(
                                                    Modifier.padding(16.dp),
                                                    horizontalArrangement =
                                                        Arrangement.spacedBy(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    CircularProgressIndicator(
                                                        Modifier.size(22.dp),
                                                        strokeWidth = 2.dp,
                                                    )
                                                    Text("A carregar a página de incorporação…")
                                                }
                                            }
                                    }
                                else ->
                                    Text(
                                        stringResource(R.string.player_unavailable),
                                        Modifier.padding(24.dp),
                                    )
                            }
                        }
                    }
                }
                if (!expanded)
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (!online)
                            Text(
                                "Sem ligação à Internet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        if (!compact)
                            Text(
                                stringResource(R.string.external_player_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted,
                            )
                        state.storageNotice?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.phase == PlayerPhase.AVAILABLE) {
                                TextButton(onClick = viewModel::retry) {
                                    Icon(Icons.Outlined.Refresh, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Repetir")
                                }
                                TextButton(onClick = viewModel::failed) {
                                    Text("O player não carregou?")
                                }
                            }
                            if (preferences?.externalBrowser == true && state.source != null)
                                TextButton(onClick = { externalDialog = true }) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.OpenInNew,
                                        null,
                                        Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text("Página oficial")
                                }
                        }
                    }
            }
            if (expanded && fullscreen.customView == null)
                FilledIconButton(
                    onClick = { expanded = false },
                    modifier = Modifier.align(Alignment.TopStart).safeDrawingPadding().padding(8.dp),
                ) {
                    Icon(Icons.Outlined.FullscreenExit, "Sair do ecrã expandido")
                }
            fullscreen.customView?.let { customView ->
                AndroidView(
                    factory = {
                        (customView.parent as? ViewGroup)?.removeView(customView)
                        customView
                    },
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                )
            }
            if (embedVisible) PlayerWindowsOverlay(windows, snackbar)
            if (windows.activeWindow == null)
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).safeDrawingPadding())
        }
        shareTarget?.let { target ->
            MediaShareSheet(target, container.embedPolicy) { shareTarget = null }
        }
        if (information)
            AlertDialog(
                onDismissRequest = { information = false },
                title = { Text("Player externo") },
                text = {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(R.string.external_player_notice))
                        Text(
                            "Os controlos, anúncios e disponibilidade pertencem ao fornecedor. Não existe medição de progresso ou PiP nativo Android. Ao sair, o iframe é destruído; voltar a abri-lo não retoma uma posição verificada."
                        )
                    }
                },
                confirmButton = { TextButton(onClick = { information = false }) { Text("Fechar") } },
            )
        if (
            state.phase == PlayerPhase.AVAILABLE &&
                preferences != null &&
                preferences?.externalPlayerConsent != true &&
                foreground
        )
            ExternalPlayerConsentDialog(
                onAccept = viewModel::grantConsent,
                onDismiss = onExit,
                storageError = preferences?.storageError == true,
                storageNotice = state.storageNotice,
            )
        if (externalDialog)
            AlertDialog(
                onDismissRequest = { externalDialog = false },
                title = { Text("Abrir a página oficial?") },
                text = {
                    Text(
                        "Será aberta a demonstração oficial do EmbedMovies no navegador. Selecciona o tipo de conteúdo e introduz o identificador TMDB ${state.details?.media?.key?.id ?: "disponível nos detalhes"}. Essa página utiliza o iframe oficial; não será aberto um link directo de vídeo. O navegador tem políticas de privacidade próprias."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            externalError =
                                !openTrustedExternal(
                                    context,
                                    "https://embedmovies.org/#playground",
                                    setOf("embedmovies.org"),
                                )
                            externalDialog = false
                        }
                    ) {
                        Text("Abrir navegador")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { externalDialog = false }) { Text("Cancelar") }
                },
            )
        if (externalError)
            AlertDialog(
                onDismissRequest = { externalError = false },
                title = { Text("Navegador indisponível") },
                text = { Text("Não foi possível abrir a página oficial.") },
                confirmButton = {
                    TextButton(onClick = { externalError = false }) { Text("Fechar") }
                },
            )
    }
}
