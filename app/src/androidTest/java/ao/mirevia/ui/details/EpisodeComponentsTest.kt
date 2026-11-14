package ao.mirevia.ui.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import ao.mirevia.core.model.Episode
import ao.mirevia.core.model.Season
import ao.mirevia.core.playback.*
import ao.mirevia.ui.theme.MireviaTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Fictional metadata and no still URLs: never creates a WebView or contacts TMDB/provider. */
class EpisodeComponentsTest {
    @get:Rule val compose = createComposeRule()
    private val episode =
        Episode(
            1,
            1,
            2,
            "Episódio fictício",
            overview = "Sinopse fictícia para verificar a expansão.",
        )
    private val source =
        VideoSource(
            "https://myembed.biz/serie/30984/1/2",
            VideoSourceType.IFRAME,
            "EmbedMovies",
            true,
            true,
            true,
            false,
        )

    @Test
    fun actionsOnlyRunOnExplicitTap() {
        var plays = 0
        var watches = 0
        var shares = 0
        compose.setContent {
            MireviaTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    EpisodeCard(
                        EpisodeWithSource(episode, source),
                        false,
                        { plays++ },
                        { watches++ },
                        { shares++ },
                    )
                }
            }
        }
        compose.runOnIdle { assertEquals(listOf(0, 0, 0), listOf(plays, watches, shares)) }
        compose.onNodeWithText("Reproduzir").performScrollTo().performClick()
        compose.onNodeWithText("Marcar visto").performScrollTo().performClick()
        compose
            .onNodeWithContentDescription("Partilhar episódio 2")
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertEquals(listOf(1, 1, 1), listOf(plays, watches, shares)) }
    }

    @Test
    fun missingSourceDisablesPlaybackAndEmbedSharingButNotManualWatchedFlag() {
        var watched = false
        compose.setContent {
            MireviaTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    EpisodeCard(EpisodeWithSource(episode, null), false, {}, { watched = true }, {})
                }
            }
        }
        compose.onNodeWithText("Reproduzir").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Partilhar episódio 2").assertIsNotEnabled()
        compose.onNodeWithText("Marcar visto").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(watched) }
    }

    @Test
    fun synopsisAndActionsRemainReachableWithLargeFonts() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MireviaTheme {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        EpisodeCard(EpisodeWithSource(episode, source), true, {}, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Ler sinopse").performScrollTo().performClick()
        compose.onNodeWithText("Recolher sinopse").assertExists()
        compose.onNodeWithText("Reproduzir").performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithContentDescription("Partilhar episódio 2")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun seasonPickerFiltersRealProvidedSeasonsAndSelectsTheirNumber() {
        var selected: Int? = null
        var dismissed = false
        compose.setContent {
            MireviaTheme {
                SeasonPickerSheet(
                    listOf(
                        Season(1, 1, "Primeira temporada fictícia"),
                        Season(2, 2, "Segunda temporada fictícia"),
                    ),
                    1,
                    { selected = it },
                    { dismissed = true },
                )
            }
        }
        compose.onNodeWithText("Procurar temporada").performTextInput("2")
        compose.onNodeWithText("Temporada 1").assertDoesNotExist()
        compose.onNodeWithText("Temporada 2").performClick()
        compose.runOnIdle {
            assertEquals(2, selected)
            assertTrue(dismissed)
        }
    }
}
