package ao.mirevia.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import ao.mirevia.core.playback.IframeRequestPolicy
import ao.mirevia.ui.theme.MireviaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExternalPlayerConsentDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun disclosesBroaderThirdPartyAccessAndCancelDoesNotGrantConsent() {
        var accepted = 0
        var dismissed = 0
        compose.setContent {
            MireviaTheme {
                ExternalPlayerConsentDialog(onAccept = { accepted++ }, onDismiss = { dismissed++ })
            }
        }
        compose.onNodeWithText("Permitir player e janelas HTTPS?").assertIsDisplayed()
        compose.onNodeWithText("O iframe começa em myembed.biz.", substring = true).assertExists()
        compose.onNodeWithText("Cloudflare Insights", substring = true).assertExists()
        compose.onNodeWithText(IframeRequestPolicy.RESOURCE_DESCRIPTION).assertExists()
        compose
            .onNodeWithText(ao.mirevia.core.playback.PlayerWindowPolicy.DESCRIPTION)
            .assertExists()
        compose.onNodeWithText("Fechar ou Voltar", substring = true).assertExists()
        compose.onNodeWithText("cookies de primeira parte", substring = true).assertExists()
        compose.onNodeWithText("HTTPS não garante", substring = true).assertExists()
        compose.runOnIdle { assertEquals(listOf(0, 0), listOf(accepted, dismissed)) }
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(listOf(0, 1), listOf(accepted, dismissed)) }
    }

    @Test
    fun acceptanceRequiresAnExplicitButtonPress() {
        var accepted = 0
        compose.setContent {
            MireviaTheme { ExternalPlayerConsentDialog(onAccept = { accepted++ }, onDismiss = {}) }
        }
        compose.runOnIdle { assertEquals(0, accepted) }
        compose.onNodeWithText("Aceitar").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, accepted) }
    }

    @Test
    fun unreadablePreferencesDisableAcceptanceWithoutDisablingCancel() {
        var accepted = 0
        var dismissed = 0
        compose.setContent {
            MireviaTheme {
                ExternalPlayerConsentDialog(
                    onAccept = { accepted++ },
                    onDismiss = { dismissed++ },
                    storageError = true,
                )
            }
        }
        compose.onNodeWithText("Aceitar").assertIsNotEnabled()
        compose.onNodeWithText("O iframe não será carregado.", substring = true).assertExists()
        compose.onNodeWithText("Cancelar").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(listOf(0, 1), listOf(accepted, dismissed)) }
    }

    @Test
    fun writeFailureNoticeRemainsVisibleAndDoesNotPretendAcceptance() {
        var accepted = 0
        val notice = "Não foi possível guardar o consentimento. O iframe não foi carregado."
        compose.setContent {
            MireviaTheme {
                ExternalPlayerConsentDialog(
                    onAccept = { accepted++ },
                    onDismiss = {},
                    storageNotice = notice,
                )
            }
        }
        compose.onNodeWithText(notice).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, accepted) }
    }

    @Test
    fun disclosureScrollsAndButtonsRemainAccessibleWithLargeFonts() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f)
            ) {
                MireviaTheme { ExternalPlayerConsentDialog(onAccept = {}, onDismiss = {}) }
            }
        }
        compose
            .onNodeWithText("Podes revogar esta autorização", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        compose.onNodeWithText("Aceitar").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").assertIsDisplayed()
    }
}
