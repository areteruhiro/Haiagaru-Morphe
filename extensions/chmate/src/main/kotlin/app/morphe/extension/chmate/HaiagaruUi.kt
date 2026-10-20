package app.morphe.extension.chmate

import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

/** Shared expressive shapes, motion, color and accessible spacing for extension screens. */
object HaiagaruUi {
    @JvmStatic
    fun controls(activity: HaiagaruComposeActivity, model: android.view.View): android.view.View =
        androidx.compose.ui.platform.ComposeView(activity).apply {
            setContent { Theme {
                var localRevision by remember(model) { mutableIntStateOf(0) }
                val revision = rememberModelRevision(model) + localRevision
                ModelContent(model, revision) { localRevision++ }
            } }
        }

    @JvmStatic
    fun showPlayer(activity: HaiagaruComposeActivity, surface: android.view.View) {
        activity.setContent { Theme {
            Surface(color = androidx.compose.ui.graphics.Color.Black) {
                androidx.compose.ui.viewinterop.AndroidView(factory = { surface }, modifier = Modifier.fillMaxSize())
            }
        } }
    }

    @JvmStatic fun bindRelease(view: android.view.View, action: Runnable) { view.setTag(0x48690b01, action) }
    @JvmStatic fun bindSwipe(view: android.view.View, action: java.util.function.Consumer<Boolean>) { view.setTag(0x48690b02, action) }
    internal class ScrollRequest(val position: Int)
    @JvmStatic fun scrollTo(view: android.view.View, position: Int) { view.setTag(0x48690b03, ScrollRequest(position.coerceAtLeast(0))) }
    @JvmStatic fun scrollPosition(view: android.view.View): Int = view.getTag(0x48690b04) as? Int ?: view.scrollY
    @JvmStatic
    fun showModel(activity: HaiagaruComposeActivity, title: String, model: android.view.View) {
        if (model is ViewGroup) prepareNativeSurfaces(model)
        activity.setContent { Theme { ModelScreen(activity, title, model) } }
    }
    @JvmStatic
    fun showArchiveSearch(activity: HaiagaruComposeActivity, model: android.widget.LinearLayout) {
        activity.setContent { Theme { ArchiveSearchScreen(activity, model) } }
    }
    @JvmStatic
    fun showForm(activity: HaiagaruComposeActivity, title: String, model: ViewGroup) {
        activity.setContent { Theme { FormScreen(activity, title, model) } }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun FormScreen(activity: HaiagaruComposeActivity, title: String, model: ViewGroup) {
        var revision by remember { mutableIntStateOf(0) }
        @Suppress("UNUSED_VARIABLE") val refresh = revision
        val rows = settingsRows(model)
        Scaffold(topBar = { TopAppBar(title = { Text(title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }, navigationIcon = {
            IconButton(onClick = { activity.finish() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") }
        }) }) { insets ->
            LazyColumn(Modifier.fillMaxSize().padding(insets).imePadding(), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(rows, key = { System.identityHashCode(it.control) }) { row ->
                    Group { BoundSettingRow(row) { revision++ } }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun Theme(content: @Composable () -> Unit) {
        val context = LocalContext.current
        val dark = isSystemInDarkTheme()
        val colors = if (Build.VERSION.SDK_INT >= 31) {
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else if (dark) darkColorScheme() else expressiveLightColorScheme()
        MaterialExpressiveTheme(colorScheme = colors, content = content)
    }

    @Composable
    fun Group(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 12.dp, top = 8.dp))
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(8.dp), content = content)
            }
        }
    }
}
