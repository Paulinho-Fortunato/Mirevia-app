package ao.mirevia.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import ao.mirevia.core.model.*
import ao.mirevia.ui.details.EpisodeCard
import ao.mirevia.ui.details.EpisodeWithSource
import ao.mirevia.ui.home.FeaturedCard
import ao.mirevia.ui.theme.MireviaTheme

/** Android Studio previews only. Fictional metadata, no images, APIs or player requests. */
@Preview(
    name = "Mirevia — destaque",
    widthDp = 360,
    showBackground = true,
    backgroundColor = 0xFF09090D,
)
@Composable
private fun FeaturePreview() {
    MireviaTheme(reduceMotion = true) {
        FeaturedCard(
            media =
                MediaSummary(
                    MediaKey(DataOrigin.DEMO, MediaType.MOVIE, 1),
                    "Depois do horizonte",
                    overview =
                        "Exemplo fictício para rever a composição visual. Não representa um filme real.",
                ),
            watchlisted = false,
            onDetails = {},
            onWatchlist = {},
        )
    }
}

@Preview(
    name = "Episódio — fonte a 2×",
    widthDp = 360,
    heightDp = 920,
    fontScale = 2f,
    showBackground = true,
    backgroundColor = 0xFF09090D,
)
@Composable
private fun LargeEpisodePreview() {
    MireviaTheme(reduceMotion = true) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            EpisodeCard(
                item =
                    EpisodeWithSource(
                        Episode(
                            1,
                            1,
                            1,
                            "Um começo fictício",
                            overview =
                                "Exemplo local para rever legibilidade e acções com fontes grandes.",
                        ),
                        null,
                    ),
                watched = false,
                onPlay = {},
                onWatched = {},
                onShare = {},
            )
        }
    }
}
