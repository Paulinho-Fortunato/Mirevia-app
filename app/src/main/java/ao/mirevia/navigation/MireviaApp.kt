package ao.mirevia.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import ao.mirevia.app.AppContainer
import ao.mirevia.core.model.*
import ao.mirevia.data.local.UserPreferences
import ao.mirevia.playback.PlayerActivity
import ao.mirevia.ui.components.*
import ao.mirevia.ui.details.*
import ao.mirevia.ui.explore.*
import ao.mirevia.ui.home.*
import ao.mirevia.ui.library.*
import ao.mirevia.ui.person.*
import ao.mirevia.ui.search.*
import ao.mirevia.ui.settings.*
import ao.mirevia.ui.theme.MireviaTheme

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
)

private val tabs =
    listOf(
        Tab("home", "Início", Icons.Outlined.Home, Icons.Filled.Home),
        Tab("explore", "Explorar", Icons.Outlined.Explore, Icons.Filled.Explore),
        Tab("search", "Pesquisa", Icons.Outlined.Search, Icons.Filled.Search),
        Tab("library", "Minha lista", Icons.Outlined.Bookmarks, Icons.Filled.Bookmarks),
        Tab("settings", "Definições", Icons.Outlined.Settings, Icons.Filled.Settings),
    )

@Composable
fun MireviaApp(container: AppContainer) {
    val preferences by
        container.preferences.preferences.collectAsStateWithLifecycle(UserPreferences())
    val online by container.network.isOnline.collectAsStateWithLifecycle(initialValue = true)
    MireviaTheme(preferences.reduceMotion, networkAvailable = online) {
        val navigation = rememberNavController()
        val backStack by navigation.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route
        val snackbar = remember { SnackbarHostState() }
        val context = LocalContext.current
        val library: LibraryViewModel =
            viewModel(factory = viewModelFactory { LibraryViewModel(container.library) })
        LaunchedEffect(library) { library.messages.collect { snackbar.showSnackbar(it) } }
        val readError by library.readError.collectAsStateWithLifecycle()
        LaunchedEffect(readError) { readError?.let { snackbar.showSnackbar(it) } }
        fun tab(route: String) {
            navigation.navigate(route) {
                popUpTo(navigation.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        fun details(key: MediaKey) {
            navigation.navigate("details/${key.origin.name}/${key.type.name}/${key.id}")
        }
        fun person(id: Int) {
            navigation.navigate("person/$id")
        }
        fun play(key: MediaKey, season: Int?, episode: Int?) {
            PlayerActivity.open(context, key, season, episode)
        }
        Scaffold(
            topBar = {
                if (!online)
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            Modifier.fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Outlined.WifiOff, null, Modifier.size(18.dp))
                            Text(
                                "Sem Internet · a tua biblioteca continua neste dispositivo.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
            },
            bottomBar = {
                if (tabs.any { it.route == currentRoute })
                    NavigationBar {
                        tabs.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { tab(item.route) },
                                icon = {
                                    Icon(if (selected) item.selectedIcon else item.icon, null)
                                },
                                label = {
                                    Text(
                                        item.label,
                                        maxLines = 2,
                                        style =
                                            MaterialTheme.typography.labelSmall.copy(
                                                letterSpacing = 0.sp
                                            ),
                                    )
                                },
                                modifier =
                                    Modifier.semantics {
                                        contentDescription = item.label
                                        stateDescription =
                                            if (selected) "Seleccionado" else "Não seleccionado"
                                    },
                            )
                        }
                    }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            NavHost(
                navigation,
                startDestination = "home",
                modifier = Modifier.fillMaxSize().padding(padding),
                enterTransition = {
                    if (preferences.reduceMotion) EnterTransition.None else fadeIn(tween(160))
                },
                exitTransition = {
                    if (preferences.reduceMotion) ExitTransition.None else fadeOut(tween(120))
                },
            ) {
                composable("home") {
                    val vm: HomeViewModel =
                        viewModel(
                            factory =
                                viewModelFactory {
                                    HomeViewModel(container.catalog, container.library)
                                }
                        )
                    HomeScreen(
                        vm,
                        library,
                        container.catalog.isDemo,
                        ::details,
                        { tab("search") },
                        { tab("explore") },
                        { play(it.media.key, it.season, it.episode) },
                    )
                }
                composable("explore") {
                    val vm: ExploreViewModel =
                        viewModel(
                            factory = viewModelFactory { ExploreViewModel(container.catalog) }
                        )
                    ExploreScreen(vm, container.catalog.isDemo, ::details)
                }
                composable("search") {
                    val vm: SearchViewModel =
                        viewModel(
                            factory =
                                viewModelFactory {
                                    SearchViewModel(container.catalog, container.library)
                                }
                        )
                    SearchScreen(
                        vm,
                        library,
                        container.catalog.isDemo,
                        ::details,
                        ::person,
                        { tab("explore") },
                    )
                }
                composable("library") { LibraryScreen(library, ::details) }
                composable("settings") {
                    SettingsScreen(container, library) { navigation.navigate("legal/$it") }
                }
                composable(
                    "details/{origin}/{type}/{id}",
                    arguments =
                        listOf(
                            navArgument("origin") { type = NavType.StringType },
                            navArgument("type") { type = NavType.StringType },
                            navArgument("id") { type = NavType.IntType },
                        ),
                ) { entry ->
                    val key =
                        runCatching {
                                MediaKey(
                                    DataOrigin.valueOf(
                                        entry.arguments?.getString("origin").orEmpty()
                                    ),
                                    MediaType.valueOf(entry.arguments?.getString("type").orEmpty()),
                                    entry.arguments?.getInt("id") ?: 0,
                                )
                            }
                            .getOrNull()
                    if (key == null)
                        StatusPanel(
                            "Conteúdo indisponível",
                            "Identificador inválido.",
                            action = "Voltar",
                            onAction = { navigation.popBackStack() },
                        )
                    else {
                        val vm: DetailsViewModel =
                            viewModel(
                                factory =
                                    viewModelFactory {
                                        DetailsViewModel(
                                            key,
                                            container.catalog,
                                            container.resolveVideoSource,
                                            container.library,
                                        )
                                    }
                            )
                        DetailsScreen(
                            vm,
                            library,
                            { navigation.popBackStack() },
                            ::details,
                            ::person,
                            ::play,
                        )
                    }
                }
                composable(
                    "person/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.IntType }),
                ) { entry ->
                    val vm: PersonViewModel =
                        viewModel(
                            factory =
                                viewModelFactory {
                                    PersonViewModel(
                                        entry.arguments?.getInt("id") ?: 0,
                                        container.catalog,
                                    )
                                }
                        )
                    PersonScreen(vm, { navigation.popBackStack() }, ::details)
                }
                composable(
                    "legal/{kind}",
                    arguments = listOf(navArgument("kind") { type = NavType.StringType }),
                ) { entry ->
                    LegalScreen(entry.arguments?.getString("kind") ?: "privacy") {
                        navigation.popBackStack()
                    }
                }
            }
        }
    }
}
