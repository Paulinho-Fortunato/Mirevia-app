package ao.mirevia

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import ao.mirevia.app.MainActivity
import ao.mirevia.app.MireviaApplication
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Run with no TMDB configuration. Tests use the explicitly labelled local demo, no live APIs. */
class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearLocalData() {
        runBlocking {
            (compose.activity.application as MireviaApplication).container.library.clearAll()
        }
    }

    @Test
    fun bottomTabsNavigateToTheirScreens() {
        compose.onNodeWithContentDescription("Explorar").performClick()
        compose.onNodeWithText("Encontra histórias à tua medida").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pesquisa").performClick()
        compose.onNodeWithText("Filmes, séries, actores e realizadores").assertIsDisplayed()
        compose.onNodeWithContentDescription("Minha lista").performClick()
        compose.onNodeWithText("A tua biblioteca").assertIsDisplayed()
        compose.onNodeWithContentDescription("Definições").performClick()
        compose.onNodeWithText("Sem contas. Sem subscrições. Só histórias.").assertIsDisplayed()
    }

    @Test
    fun demoDetailCannotOfferFakePlayback() {
        compose.onNodeWithText("Ver detalhes").performScrollTo().performClick()
        compose.onNodeWithText("Reprodução indisponível").assertIsNotEnabled()
        compose
            .onNodeWithContentDescription("Adicionar aos favoritos")
            .performScrollTo()
            .performClick()
        compose.onNodeWithContentDescription("Remover dos favoritos").assertExists()
    }
}
