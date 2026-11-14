package ao.mirevia.ui.person

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ao.mirevia.core.model.MediaKey
import ao.mirevia.ui.components.*
import ao.mirevia.ui.search.MediaSearchRow
import ao.mirevia.ui.theme.Muted
import coil3.compose.AsyncImage

@Composable
fun PersonScreen(viewModel: PersonViewModel, onBack: () -> Unit, onDetails: (MediaKey) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { ScreenHeader("Pessoas", onBack = onBack) }
        when (val current = state) {
            LoadState.Loading -> item { CatalogSkeleton() }
            is LoadState.Error -> item { ErrorPanel(current.error, viewModel::reload) }
            is LoadState.Data -> {
                val details = current.value
                item {
                    Column(
                        Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.width(130.dp)
                                .height(175.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (details.person.profileUrl != null)
                                AsyncImage(
                                    details.person.profileUrl,
                                    "Retrato de ${details.person.name}",
                                    Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            else Icon(Icons.Outlined.PersonOutline, null)
                        }
                        Text(details.person.name, style = MaterialTheme.typography.headlineLarge)
                        Text(
                            when (details.person.department) {
                                "Acting" -> "Interpretação"
                                "Directing" -> "Realização"
                                else -> details.person.department ?: "Informação não disponível"
                            },
                            color = Muted,
                        )
                        Text(
                            "Nascimento: ${details.birthday ?: "Informação não disponível"}",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Local: ${details.birthplace ?: "Informação não disponível"}",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            details.biography ?: "Informação não disponível",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text("Filmografia", style = MaterialTheme.typography.titleLarge)
                    }
                }
                if (details.filmography.isEmpty())
                    item {
                        Text(
                            "Informação não disponível",
                            Modifier.padding(horizontal = 20.dp),
                            color = Muted,
                        )
                    }
                items(details.filmography, key = { it.key.storageKey }) { media ->
                    MediaSearchRow(media, { onDetails(media.key) })
                }
            }
        }
    }
}
