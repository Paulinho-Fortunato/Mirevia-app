package ao.mirevia.ui.search

import androidx.compose.material3.Text
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import ao.mirevia.core.model.DataOrigin
import ao.mirevia.core.model.MediaKey
import ao.mirevia.core.model.MediaSummary
import ao.mirevia.core.model.MediaType
import ao.mirevia.ui.theme.MireviaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Local fictional content only: no TMDB, image requests or player contact. */
class MediaSearchRowTest {
    @get:Rule val compose = createComposeRule()

    private val media =
        MediaSummary(
            key = MediaKey(DataOrigin.DEMO, MediaType.MOVIE, 1),
            title = "Resultado fictício de teste",
        )

    @Test
    fun clickingResultSelectsItsMediaKey() {
        var selected: MediaKey? = null
        compose.setContent {
            MireviaTheme {
                MediaSearchRow(media = media, onClick = { selected = media.key })
            }
        }

        compose.onNodeWithText(media.title).assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(media.key, selected) }
    }

    @Test
    fun subtitleAndTrailingContentDoNotInvokeClickDuringComposition() {
        var clicks = 0
        val subtitle = "Subtítulo fictício de teste"
        compose.setContent {
            MireviaTheme {
                MediaSearchRow(
                    media = media,
                    onClick = { clicks++ },
                    trailing = { Text("Acção de teste") },
                    subtitle = subtitle,
                )
            }
        }

        compose.onNodeWithText(subtitle).assertIsDisplayed()
        compose.onNodeWithText("Acção de teste").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, clicks) }
        compose.onNodeWithText(media.title).performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
