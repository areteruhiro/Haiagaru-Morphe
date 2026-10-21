package app.morphe.extension.chmate

import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput

/** Observe legacy controllers without attaching their form widgets or replacing listeners. */
@Composable internal fun rememberModelRevision(vararg roots: View?): Int {
    var revision by remember(*roots) { mutableIntStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(*roots) {
        var previous = Long.MIN_VALUE
        while (true) {
            if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) { delay(200); continue }
            val value = roots.fold(7L) { hash, root -> hash * 31 + modelFingerprint(root) }
            if (value != previous) { previous = value; revision++ }
            delay(200)
        }
    }
    return revision
}

private fun modelFingerprint(view: View?): Long {
    if (view == null) return 0L
    var value = System.identityHashCode(view).toLong()
    value = value * 31 + System.identityHashCode(view.getTag(0x48690b03))
    value = value * 31 + (view.getTag(EddiArchiveSearchUi.CONTROL_SCALE_TAG)?.hashCode() ?: 0)
    value = value * 31 + (view.getTag(EddiArchiveSearchUi.RESULT_SCALE_TAG)?.hashCode() ?: 0)
    value = value * 31 + view.visibility + if (view.isEnabled) 1 else 0
    when (view) {
        is TextView -> value = value * 31 + view.text.hashCode() + (view.error?.hashCode() ?: 0)
        is ImageView -> value = value * 31 + System.identityHashCode(view.drawable)
        is ProgressBar -> value = value * 31 + view.progress
    }
    if (view is CompoundButton) value = value * 31 + if (view.isChecked) 1 else 0
    if (view is AdapterView<*>) {
        value = value * 31 + System.identityHashCode(view.adapter) + (view.adapter?.count ?: 0)
        return value * 31 + view.selectedItemPosition
    }
    if (view is NativeSurfaceSlot) return value
    if (view is ViewGroup) for (index in 0 until view.childCount) value = value * 31 + modelFingerprint(view.getChildAt(index))
    return value
}

/** Only WebView and platform playback/canvas content use View interop. */
internal class NativeSurfaceSlot(val surface: View) : View(surface.context)

internal fun weightedSurface(root: View?): Int? = if (root is LinearLayout)
    (0 until root.childCount).firstOrNull {
        val child = root.getChildAt(it)
        child is NativeSurfaceSlot && child.visibility == View.VISIBLE &&
            (child.layoutParams as? LinearLayout.LayoutParams)?.weight?.let { weight -> weight > 0 } == true
    } else null

/** Bounded browser plus independently scrolling controls, also used by web dialogs. */
@Composable internal fun BrowserModel(root: LinearLayout, revision: Int, modifier: Modifier, changed: () -> Unit) {
    val index = weightedSurface(root) ?: return
    BoxWithConstraints(modifier) {
        val controlsHeight = maxHeight * .30f
        Column(Modifier.fillMaxSize()) {
            if (index > 0) Column(Modifier.fillMaxWidth().heightIn(max = controlsHeight)
                .verticalScroll(rememberScrollState())) {
                for (i in 0 until index) ModelContent(root.getChildAt(i), revision, changed = changed)
            }
            ModelContent(root.getChildAt(index), revision, Modifier.weight(1f), changed = changed)
            if (index + 1 < root.childCount) Column(Modifier.fillMaxWidth().heightIn(max = controlsHeight)
                .verticalScroll(rememberScrollState())) {
                for (i in index + 1 until root.childCount) ModelContent(root.getChildAt(i), revision, changed = changed)
            }
        }
    }
}

internal fun prepareNativeSurfaces(root: ViewGroup) {
    for (index in 0 until root.childCount) {
        val child = root.getChildAt(index)
        if (child is NativeSurfaceSlot) continue
        val browserFrame = child is FrameLayout && ((0 until child.childCount).any { child.getChildAt(it) is android.webkit.WebView } || (child.layoutParams as? LinearLayout.LayoutParams)?.weight?.let { it > 0 } == true)
        if (child is android.webkit.WebView || child is VideoView || browserFrame ||
            child.javaClass.name.endsWith("GifViewerActivity\$GifCanvas")) {
            val params = child.layoutParams
            root.removeViewAt(index)
            root.addView(NativeSurfaceSlot(child).apply { visibility = child.visibility }, index, params)
        } else if (child is ViewGroup) prepareNativeSurfaces(child)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable internal fun ModelContent(view: View, revision: Int = 0, modifier: Modifier = Modifier,
    insideScroll: Boolean = false, changed: () -> Unit = {}) {
    if (view.visibility != View.VISIBLE) return
    when (view) {
        is NativeSurfaceSlot -> AndroidView(factory = { view.surface },
            modifier = if (view.layoutParams?.width == 1 && view.layoutParams?.height == 1) Modifier.size(1.dp)
                else if ((view.layoutParams as? LinearLayout.LayoutParams)?.weight?.let { it > 0 } == true)
                    modifier.fillMaxSize()
                else modifier.fillMaxWidth().height(420.dp))
        is ListView -> {
            val adapter = view.adapter ?: return
            DisposableEffect(adapter) {
                val observer = object : android.database.DataSetObserver() {
                    override fun onChanged() { changed() }
                    override fun onInvalidated() { changed() }
                }
                adapter.registerDataSetObserver(observer)
                onDispose { adapter.unregisterDataSetObserver(observer) }
            }
            androidx.compose.foundation.lazy.LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(adapter.count) { index ->
                    val row = adapter.getView(index, null, view)
                    val label = adapter.getItem(index)?.toString().orEmpty()
                    val selectable = adapter.isEnabled(index) && view.isEnabled
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth().combinedClickable(enabled = selectable,
                            onClick = {
                                if (view.choiceMode != ListView.CHOICE_MODE_NONE)
                                    view.setItemChecked(index, if (view.choiceMode == ListView.CHOICE_MODE_SINGLE) true else !view.isItemChecked(index))
                                view.performItemClick(row, index, adapter.getItemId(index)); changed()
                            },
                            onLongClick = { view.onItemLongClickListener?.onItemLongClick(view, row, index, adapter.getItemId(index)); changed() })) {
                        Column(Modifier.padding(12.dp)) {
                            if (view.choiceMode != ListView.CHOICE_MODE_NONE) Checkbox(view.isItemChecked(index), onCheckedChange = null)
                            if (row is TextView) Text(row.text.toString(), style = MaterialTheme.typography.bodyMedium)
                            else ModelContent(row, revision, changed = changed)
                            if (row !is TextView && row !is ViewGroup && label.isNotBlank()) Text(label)
                        }
                    }
                }
            }
        }
        is SeekBar -> {
            Slider(value = view.progress.toFloat().coerceIn(0f, view.max.coerceAtLeast(1).toFloat()),
                onValueChange = { value -> view.progress = value.toInt(); changed() },
                valueRange = 0f..view.max.coerceAtLeast(1).toFloat(), enabled = view.isEnabled)
        }
        is ProgressBar -> {
            if (view.isIndeterminate) LinearProgressIndicator(Modifier.fillMaxWidth())
            else LinearProgressIndicator(progress = { view.progress.toFloat() / view.max.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
        }
        is Spinner, is TextView, is ImageView -> {
            val label = if (view is EditText) view.hint?.toString().orEmpty()
                else if (view is TextView) view.text.toString() else view.contentDescription?.toString().orEmpty()
            BoundSettingRow(BoundSetting(view, label), changed)
        }
        is LinearLayout -> if (view.orientation == LinearLayout.HORIZONTAL &&
            (0 until view.childCount).all { (view.getChildAt(it) is Button && view.getChildAt(it) !is CompoundButton) || view.getChildAt(it).javaClass == View::class.java }) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (index in 0 until view.childCount) {
                    val button = view.getChildAt(index) as? Button ?: continue
                    if (button.visibility == View.VISIBLE) {
                        val release = button.getTag(0x48690b01) as? Runnable
                        if (release == null) TextButton(onClick = { button.performClick(); changed() }, enabled = button.isEnabled) { Text(button.text.toString()) }
                        else Text(button.text.toString(), style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(12.dp).pointerInput(button) {
                                detectTapGestures(onTap = { if (button.isEnabled) { button.performClick(); changed() } },
                                    onLongPress = { if (button.isEnabled) { button.performLongClick(); changed() } },
                                    onPress = { try { awaitRelease() } finally { release.run(); changed() } })
                            })
                    }
                }
            }
        } else ModelChildren(view, revision, insideScroll, changed)
        is ScrollView -> {
            if (insideScroll) {
                ModelChildren(view, revision, true, changed)
                return
            }
            val scroll = rememberScrollState()
            var horizontal by remember(view) { mutableFloatStateOf(0f) }
            @Suppress("UNCHECKED_CAST")
            val swipe = view.getTag(0x48690b02) as? java.util.function.Consumer<Boolean>
            val gestures = if (swipe == null) Modifier else Modifier.pointerInput(view) {
                detectHorizontalDragGestures(
                    onDragStart = { horizontal = 0f },
                    onDragEnd = { if (kotlin.math.abs(horizontal) > 96.dp.toPx()) swipe.accept(horizontal > 0) },
                    onDragCancel = { horizontal = 0f },
                    onHorizontalDrag = { _, amount -> horizontal += amount })
            }
            val request = view.getTag(0x48690b03) as? HaiagaruUi.ScrollRequest
            LaunchedEffect(view, request) { if (request != null) scroll.scrollTo(request.position) }
            LaunchedEffect(view) { snapshotFlow { scroll.value }.collect { view.setTag(0x48690b04, it) } }
            Column(modifier.fillMaxWidth().heightIn(max = 600.dp).then(gestures).verticalScroll(scroll)) {
                ModelChildren(view, revision, true, changed)
            }
        }
        is ViewGroup -> ModelChildren(view, revision, insideScroll, changed)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable private fun ModelChildren(view: ViewGroup, revision: Int, insideScroll: Boolean = false, changed: () -> Unit) {
    // Result cards use a listener on their LinearLayout, not on the title TextView.
    val click = if (!view.hasOnClickListeners()) Modifier else Modifier.combinedClickable(
        enabled = view.isEnabled, onClick = { view.performClick(); changed() },
        onLongClick = { view.performLongClick(); changed() })
    Column(Modifier.fillMaxWidth().then(click), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Pair explanatory labels with editable fields while retaining composite controls and lists.
            var index = 0
            while (index < view.childCount) {
                val child = view.getChildAt(index++)
                val next = view.getChildAt(index)
                if (child is TextView && child !is Button && child !is EditText && !child.hasOnClickListeners() &&
                    (next is EditText || next is Spinner) && child.visibility == View.VISIBLE) {
                    index++
                    if (next.visibility == View.VISIBLE)
                        BoundSettingRow(BoundSetting(next, child.text.toString()), changed)
                } else ModelContent(child, revision, insideScroll = insideScroll, changed = changed)
            }
    }
}

/** Bounded result viewport: do not nest it in ModelScreen's scrolling form. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ArchiveSearchScreen(activity: HaiagaruComposeActivity, root: LinearLayout) {
    var localRevision by remember(root) { mutableIntStateOf(0) }
    val revision = rememberModelRevision(root) + localRevision
    val changed = { localRevision++; Unit }
    val appearance = (root.getChildAt(0) as LinearLayout).getChildAt(1) as Button
    val density = LocalDensity.current
    val controlScale = EddiArchiveSearchUi.clampTextScale(root.getTag(EddiArchiveSearchUi.CONTROL_SCALE_TAG) as? Int ?: 100)
    val resultScale = EddiArchiveSearchUi.clampTextScale(root.getTag(EddiArchiveSearchUi.RESULT_SCALE_TAG) as? Int ?: 100)
    val controlsDensity = Density(density.density, density.fontScale * controlScale / 100f)
    val resultsDensity = Density(density.density, density.fontScale * resultScale / 100f)
    Scaffold(topBar = { CompositionLocalProvider(LocalDensity provides controlsDensity) { TopAppBar(title = { Text("エッヂ過去ログ検索") }, navigationIcon = {
        TextButton(onClick = { activity.onBackPressed() }) { Text("戻る") }
    }, actions = { TextButton(onClick = { appearance.performClick(); changed() }) { Text(appearance.text.toString()) } }) } }) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets).imePadding().padding(horizontal = 16.dp)) {
            val formHeight = maxHeight * .40f
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CompositionLocalProvider(LocalDensity provides controlsDensity) {
                Column(Modifier.fillMaxWidth().heightIn(max = formHeight).verticalScroll(rememberScrollState())) {
                    val form = root.getChildAt(1) as LinearLayout
                    if (form.visibility == View.VISIBLE) for (index in 0 until form.childCount) {
                        // The instructions above the keyword are not its input label.
                        ModelContent(form.getChildAt(index), revision, changed = changed)
                    }
                }
                Text((root.getChildAt(2) as TextView).text.toString(),
                    style = MaterialTheme.typography.bodySmall, maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                CompositionLocalProvider(LocalDensity provides resultsDensity) {
                ModelContent(root.getChildAt(3), revision, Modifier.weight(1f), changed = changed)
                }
                CompositionLocalProvider(LocalDensity provides controlsDensity) {
                ModelContent(root.getChildAt(4), revision, changed = changed)
                val original = root.getChildAt(5) as Button
                TextButton(onClick = { original.performClick(); changed() }) {
                    Text(original.text.toString(), maxLines = 1)
                }
                }
            }
        }
    }
}

/** Compose shell for controller-built tools, including asynchronous status and list changes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun ModelScreen(activity: HaiagaruComposeActivity, title: String, root: View) {
    var localRevision by remember(root) { mutableIntStateOf(0) }
    val revision = rememberModelRevision(root) + localRevision
    if (root is ViewGroup) prepareNativeSurfaces(root)
    @Suppress("UNUSED_VARIABLE") val observed = revision
    Scaffold(topBar = { TopAppBar(title = { Text(title) }, navigationIcon = {
        TextButton(onClick = { activity.onBackPressed() }) { Text("戻る") }
    }) }) { insets ->
        val surfaceIndex = if (root is LinearLayout) (0 until root.childCount).firstOrNull {
            val child = root.getChildAt(it)
            child is NativeSurfaceSlot && (child.layoutParams as? LinearLayout.LayoutParams)?.weight?.let { weight -> weight > 0 } == true
        } else null
        if (root is LinearLayout && surfaceIndex != null) {
            // A WebView owns its vertical scrolling. Never measure it inside verticalScroll.
            BoxWithConstraints(Modifier.fillMaxSize().padding(insets).imePadding()) {
                val controlsHeight = maxHeight * .35f
                Column(Modifier.fillMaxSize()) {
                    if (surfaceIndex > 0) Column(Modifier.fillMaxWidth().heightIn(max = controlsHeight)
                        .verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                        for (index in 0 until surfaceIndex) ModelContent(root.getChildAt(index), revision) { localRevision++ }
                    }
                    ModelContent(root.getChildAt(surfaceIndex), revision, Modifier.weight(1f)) { localRevision++ }
                    if (surfaceIndex + 1 < root.childCount) Column(Modifier.fillMaxWidth().heightIn(max = controlsHeight)
                        .verticalScroll(rememberScrollState())) {
                        for (index in surfaceIndex + 1 until root.childCount) ModelContent(root.getChildAt(index), revision) { localRevision++ }
                    }
                }
            }
        } else {
        Column(Modifier.fillMaxSize().padding(insets).imePadding().verticalScroll(rememberScrollState())
            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HaiagaruUi.Group { ModelContent(root, revision) { localRevision++ } }
        }
        }
    }
}
