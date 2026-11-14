package ao.mirevia.playback

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ao.mirevia.app.MireviaApplication
import ao.mirevia.core.model.*
import ao.mirevia.ui.components.viewModelFactory
import ao.mirevia.ui.player.PlayerScreen
import ao.mirevia.ui.player.PlayerViewModel

class PlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        val container = (application as MireviaApplication).container
        val key =
            runCatching {
                    MediaKey(
                        DataOrigin.valueOf(intent.getStringExtra("origin").orEmpty()),
                        MediaType.valueOf(intent.getStringExtra("type").orEmpty()),
                        intent.getIntExtra("id", 0),
                    )
                }
                .getOrNull()
        val season = intent.getIntExtra("season", -1).takeUnless { it == -1 }
        val episode = intent.getIntExtra("episode", -1).takeUnless { it == -1 }
        setContent {
            val vm: PlayerViewModel =
                viewModel(
                    factory = viewModelFactory { PlayerViewModel(key, season, episode, container) }
                )
            PlayerScreen(vm, container, ::finish, ::setPlayerImmersiveMode)
        }
    }

    private fun setPlayerImmersiveMode(enabled: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (enabled) hide(WindowInsetsCompat.Type.systemBars())
            else show(WindowInsetsCompat.Type.systemBars())
        }
    }

    companion object {
        fun open(context: Context, key: MediaKey, season: Int?, episode: Int?) {
            context.startActivity(
                Intent(context, PlayerActivity::class.java)
                    .putExtra("origin", key.origin.name)
                    .putExtra("type", key.type.name)
                    .putExtra("id", key.id)
                    .putExtra("season", season ?: -1)
                    .putExtra("episode", episode ?: -1)
            )
        }
    }
}
