package ao.mirevia.ui.player

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import ao.mirevia.core.playback.*
import ao.mirevia.ui.theme.MireviaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlayerWindowSurfaceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun identifiesThirdPartyWindowAndShowsOnlyTheOrigin() {
        val origin =
            PlaybackBlock.request(
                PlaybackBlockKind.NAVIGATION,
                "https://third-party.example:443/private/secret-path?token=secret-token",
            )
        compose.setContent {
            MireviaTheme {
                PlayerWindowSurface(origin, false, null, {}) { Text("Local content fixture") }
            }
        }
        compose.onNodeWithText("Janela do player · terceiros").assertIsDisplayed()
        compose.onNodeWithText("Origem: https://third-party.example:443").assertIsDisplayed()
        compose.onNodeWithText("secret-path", substring = true).assertDoesNotExist()
        compose.onNodeWithText("secret-token", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Local content fixture").assertIsDisplayed()
    }

    @Test
    fun closeIsExplicitAndNotInvokedDuringComposition() {
        var closed = 0
        compose.setContent {
            MireviaTheme {
                PlayerWindowSurface(null, false, null, { closed++ }) { Text("Local fixture") }
            }
        }
        compose.runOnIdle { assertEquals(0, closed) }
        compose.onNodeWithText("Fechar").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, closed) }
    }

    @Test
    fun unknownDestinationIsNotPresentedAsASecurityBlockOrTrustedSite() {
        compose.setContent {
            MireviaTheme {
                PlayerWindowSurface(null, false, null, {}) { Text("Blank context fixture") }
            }
        }
        compose.onNodeWithText("Endereço ainda não disponível").assertIsDisplayed()
        compose.onNodeWithText("Player interrompido").assertDoesNotExist()
        compose.onNodeWithText("Blank context fixture").assertIsDisplayed()
    }

    @Test
    fun failedWindowReplacesOnlyItsOwnContentAndStillHasClose() {
        compose.setContent {
            MireviaTheme {
                PlayerWindowSurface(null, false, "Falha da janela de teste", {}) {
                    Text("Remote content")
                }
            }
        }
        compose.onNodeWithText("Falha da janela de teste").assertExists()
        compose.onNodeWithText("Remote content").assertDoesNotExist()
        compose.onNodeWithText("Fechar").assertIsDisplayed()
    }

    @Test
    fun closeRemainsAccessibleAtDoubleFontSize() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f)
            ) {
                MireviaTheme { PlayerWindowSurface(null, false, null, {}) { Text("Fixture") } }
            }
        }
        compose.onNodeWithText("Fechar").assertIsDisplayed().assertHasClickAction()
    }
}
