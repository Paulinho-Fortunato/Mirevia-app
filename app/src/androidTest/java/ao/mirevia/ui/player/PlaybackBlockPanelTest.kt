package ao.mirevia.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ao.mirevia.core.playback.*
import ao.mirevia.ui.theme.MireviaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaybackBlockPanelTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun showsOnlyOriginAndLeavesCopyShareAndRetryUnderUserControl() {
        val block =
            PlaybackBlock.request(
                PlaybackBlockKind.RESOURCE,
                "https://cdn.example:8443/private/secret-path?token=secret-token",
            )
        var copies = 0
        var shares = 0
        var retries = 0
        compose.setContent {
            MireviaTheme {
                PlaybackBlockPanel(
                    block,
                    EmbedUrlPolicy(setOf("myembed.biz")),
                    { retries++ },
                    { copies++ },
                    { shares++ },
                    Modifier.fillMaxSize(),
                )
            }
        }
        compose.onNodeWithText("Domínio bloqueado: cdn.example").assertExists()
        compose.onNodeWithText("Porta explícita: 8443").assertExists()
        compose.onNodeWithText("secret-token", substring = true).assertDoesNotExist()
        compose.onNodeWithText("secret-path", substring = true).assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(0, 0, 0), listOf(copies, shares, retries)) }
        compose.onNodeWithText("Copiar diagnóstico").performScrollTo().performClick()
        compose.onNodeWithText("Partilhar diagnóstico").performScrollTo().performClick()
        compose.onNodeWithText("Tentar novamente").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(1, 1, 1), listOf(copies, shares, retries)) }
    }
}
