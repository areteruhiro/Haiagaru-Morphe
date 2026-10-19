package app.morphe.extension.chmate

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.MediaController
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** VideoView still owns decoding and gestures; its controls are Compose. */
open class HaiagaruMediaController(context: Context) : MediaController(context) {
    private var player: MediaPlayerControl? = null
    private var visible by mutableStateOf(false)
    private var host: FrameLayout? = null
    private var panel: ComposeView? = null
    override fun setMediaPlayer(value: MediaPlayerControl) { player = value }
    override fun setAnchorView(anchor: View) {
        panel?.let { host?.removeView(it) }
        host = anchor as? FrameLayout
        panel = ComposeView(context).apply {
            setContent { HaiagaruUi.Theme { PlayerControls() } }
        }
        host?.addView(panel, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
    }
    override fun show() { show(0) }
    override fun show(timeout: Int) { visible = true; panel?.visibility = View.VISIBLE }
    override fun hide() { visible = false; panel?.visibility = View.GONE }
    override fun isShowing() = visible

    @Composable private fun PlayerControls() {
        if (!visible) return
        var position by remember { mutableIntStateOf(0) }
        var duration by remember { mutableIntStateOf(0) }
        var playing by remember { mutableStateOf(false) }
        var seeking by remember { mutableStateOf(false) }
        LaunchedEffect(visible) {
            while (visible) {
                try {
                    duration = player?.duration?.coerceAtLeast(0) ?: 0
                    if (!seeking) position = player?.currentPosition?.coerceAtLeast(0) ?: 0
                    playing = player?.isPlaying ?: false
                } catch (_: RuntimeException) { }
                delay(250)
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .94f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { try { player?.seekTo((position - 10000).coerceAtLeast(0)) } catch (_: RuntimeException) {} }) { Text("10秒戻る") }
                    FilledTonalButton(onClick = { try { if (playing) player?.pause() else player?.start(); playing = !playing } catch (_: RuntimeException) {} }) {
                        Text(if (playing) "一時停止" else "再生")
                    }
                    TextButton(onClick = { try { player?.seekTo((position + 10000).coerceAtMost(duration)) } catch (_: RuntimeException) {} }) { Text("10秒進む") }
                }
                Slider(value = position.toFloat().coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                    onValueChange = { seeking = true; position = it.toInt() },
                    onValueChangeFinished = { try { player?.seekTo(position) } catch (_: RuntimeException) {}; seeking = false },
                    valueRange = 0f..duration.coerceAtLeast(1).toFloat(), enabled = duration > 0)
                Text("${time(position)} / ${time(duration)}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
    private fun time(millis: Int): String = "%d:%02d".format(millis / 60000, millis / 1000 % 60)
}
