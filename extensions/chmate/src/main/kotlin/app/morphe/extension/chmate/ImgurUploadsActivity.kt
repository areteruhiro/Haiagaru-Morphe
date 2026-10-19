package app.morphe.extension.chmate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/** Account media remain behind the existing session, pagination and delete policy. */
class ImgurUploadsActivity : HaiagaruComposeActivity() {
    private var page by mutableIntStateOf(0)
    private var loading by mutableStateOf(false)
    private var deleting by mutableStateOf(false)
    private var status by mutableStateOf("")
    private var error by mutableStateOf<String?>(null)
    private var rows by mutableStateOf<List<JSONObject>>(emptyList())
    private var threads by mutableStateOf<Map<String, List<String>>>(emptyMap())
    private var selected by mutableStateOf<Set<String>>(emptySet())
    private var snapshot: ImgurWebUpload.UploadPage? = null
    private var confirm by mutableStateOf(false)
    private var result by mutableStateOf<String?>(null)
    private var generation = 0
    @Volatile private var stopped = false
    private val thumbnails = ImgurThumbnailLoader()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (!ImgurWebUpload.supported(this)) { finish(); return }
        ImgurWebUpload.init(this)
        page = state?.getInt("page", 0)?.coerceIn(0, 10000) ?: 0
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { closeScreen() }
        })
        setContent { HaiagaruUi.Theme { UploadsScreen() } }
    }

    override fun onResume() { super.onResume(); if (!deleting) load(page) }
    override fun onSaveInstanceState(out: Bundle) { out.putInt("page", page); super.onSaveInstanceState(out) }
    override fun onDestroy() { stopped = true; generation++; thumbnails.close(); super.onDestroy() }
    private fun closeScreen() {
        if (deleting) Toast.makeText(this@ImgurUploadsActivity, "削除中です。結果が出るまでお待ちください", Toast.LENGTH_SHORT).show()
        else finish()
    }

    private fun load(next: Int) {
        if (deleting) return
        page = next.coerceIn(0, 10000)
        val requested = page
        val token = ++generation
        thumbnails.reset(); selected = emptySet(); rows = emptyList(); snapshot = null
        loading = true; error = null
        Thread({
            try {
                val response = ImgurWebUpload.uploadPage(requested)
                val items = (0 until response.rows.length()).mapNotNull { response.rows.optJSONObject(it) }
                val related = ImgurUploadHistory.threads(this, response.accountId)
                runOnUiThread {
                    if (!isFinishing && !isDestroyed && token == generation) {
                        snapshot = response; rows = items; threads = related; loading = false
                    }
                }
            } catch (failure: Exception) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed && token == generation) {
                        loading = false
                        error = "投稿一覧を取得できませんでした。ログイン状態と通信を確認し、再読み込みしてください。"
                    }
                }
            }
        }, "Haiagaru-Imgur-uploads").start()
    }

    private fun deleteSelected() {
        val original = snapshot ?: return
        val ids = selected.toTypedArray()
        if (deleting || ids.isEmpty()) return
        deleting = true; stopped = false; confirm = false
        status = "Imgurから削除しています…"
        Thread({
            val summary = try {
                val deleted = ImgurWebUpload.deleteUploads(original, ids) { stopped }
                "削除完了：${deleted.deleted}件\n結果未確認：${deleted.unconfirmed}件\n未実行：${deleted.notAttempted}件" +
                    if (deleted.unconfirmed > 0) "\n通信が切れた場合も削除済みの可能性があります。再実行する前に一覧を確認してください。" else ""
            } catch (failure: Exception) {
                "削除を開始できませんでした。ログイン状態を確認し、一覧を更新してください。"
            }
            runOnUiThread {
                deleting = false
                if (!isFinishing && !isDestroyed) { status = summary; result = summary; load(page) }
            }
        }, "Haiagaru-Imgur-delete").start()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun UploadsScreen() {
        val busy = loading || deleting
        val eligible = rows.map { it.optString("id") }.filter { ImgurWebPolicy.imageId(it) }.toSet()
        Scaffold(topBar = {
            TopAppBar(title = { Text("Imgur投稿一覧") }, navigationIcon = {
                IconButton(onClick = { closeScreen() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "戻る") }
            })
        }, bottomBar = {
            if (selected.isNotEmpty()) Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${selected.size}件を選択", Modifier.weight(1f))
                    Button(onClick = { confirm = true }, enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("削除…") }
                }
            }
        }) { insets ->
            LazyColumn(Modifier.fillMaxSize().padding(insets), contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    HaiagaruUi.Group {
                        Text("ログイン中のアカウントの投稿", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))
                        Text("非公開の画像・動画も表示します。更新やページ移動で選択は解除されます。", Modifier.padding(8.dp))
                        Row {
                            TextButton(onClick = { startActivity(Intent(this@ImgurUploadsActivity, ImgurWebLoginActivity::class.java)) }, enabled = !busy) { Text("ログイン・設定") }
                            TextButton(onClick = { load(page) }, enabled = !busy) { Text("更新") }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { load(page - 1) }, enabled = !busy && page > 0) { Text("前へ") }
                        Text("${page + 1}ページ")
                        TextButton(onClick = { load(page + 1) }, enabled = !busy && rows.isNotEmpty() && page < 10000) { Text("次へ") }
                    }
                }
                if (busy) item {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(Modifier.size(28.dp)); Text(if (deleting) status else "投稿を読み込んでいます…")
                    }
                }
                error?.let { message -> item { HaiagaruUi.Group {
                    Text(message, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { load(page) }, enabled = !busy) { Text("再読み込み") }
                } } }
                if (!busy && error == null && rows.isEmpty()) item { Text("このページに投稿はありません。", Modifier.padding(16.dp)) }
                if (rows.isNotEmpty()) item {
                    Row(Modifier.fillMaxWidth().clickable(enabled = !busy) {
                        selected = if (selected == eligible) emptySet() else eligible
                    }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = eligible.isNotEmpty() && selected == eligible, enabled = !busy, onCheckedChange = null)
                        Text("このページを全選択")
                    }
                }
                itemsIndexed(rows, key = { index, row -> "$generation:$index:${row.optString("id")}" }) { _, row -> MediaCard(row, busy) }
            }
        }
        if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("${selected.size}件をImgurから削除しますか？") },
            text = { Text("画像・動画をサーバーから削除します。取り消せません。貼り付け済みのURLからも見られなくなります。\n\n端末内の元ファイルは残ります。") },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("キャンセル") } },
            confirmButton = { TextButton(onClick = { deleteSelected() }) { Text("サーバーから削除", color = MaterialTheme.colorScheme.error) } })
        result?.let { message -> AlertDialog(onDismissRequest = { result = null }, title = { Text("削除結果") },
            text = { Text(message) }, confirmButton = { TextButton(onClick = { result = null }) { Text("閉じる") } }) }
    }

    @Composable private fun MediaCard(row: JSONObject, busy: Boolean) {
        val id = row.optString("id")
        val url = row.optString("link")
        val pending = ImgurWebPolicy.processing(row.opt("processing"))
        val selectable = ImgurWebPolicy.imageId(id)
        val title = row.optString("title").takeUnless { it.isBlank() || it == "null" } ?: id
        var preview by remember(id, url) { mutableStateOf<Bitmap?>(null) }
        val previewUrl = ImgurWebPolicy.thumbnailUrl(id, url, pending)
        DisposableEffect(previewUrl) {
            var active = true
            thumbnails.loadPreview(previewUrl) { if (active) preview = it }
            onDispose { active = false }
        }
        HaiagaruUi.Group {
            Row(Modifier.fillMaxWidth().clickable(enabled = !busy && selectable) {
                selected = if (id in selected) selected - id else selected + id
            }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                preview?.let { Image(it.asImageBitmap(), "投稿サムネイル", Modifier.size(88.dp), contentScale = ContentScale.Fit) }
                    ?: Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) { Text(if (pending) "処理中" else "プレビューなし", style = MaterialTheme.typography.labelSmall) }
                Column(Modifier.weight(1f).padding(12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    val millis = row.optLong("datetime") * 1000L
                    if (millis > 0) Text(android.text.format.DateFormat.getDateFormat(this@ImgurUploadsActivity).format(java.util.Date(millis)), style = MaterialTheme.typography.bodySmall)
                    Text(if (pending) "処理中・更新して確認" else if (row.optString("type").startsWith("video/") || url.endsWith(".mp4") || url.endsWith(".webm")) "動画" else "画像", style = MaterialTheme.typography.labelMedium)
                }
                Checkbox(checked = id in selected, onCheckedChange = null, enabled = !busy && selectable)
            }
            if (url.isNotBlank()) Text(url, Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = {
                (getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText("Imgur URL", url))
                Toast.makeText(this@ImgurUploadsActivity, "URLをコピーしました", Toast.LENGTH_SHORT).show()
            }, enabled = !busy && !pending && ImgurWebPolicy.mediaUrl(url)) { Text("URLをコピー") }
            if (ImgurUploadHistory.threadsEnabled(this@ImgurUploadsActivity)) threads[url]?.takeIf { it.isNotEmpty() }?.let { related ->
                Text("アップロード元スレ（書き込み完了は未確認）", Modifier.padding(12.dp), style = MaterialTheme.typography.labelMedium)
                related.forEach { thread -> TextButton(onClick = {
                    try { startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(thread)).setClass(this@ImgurUploadsActivity, OpenUrlActivity::class.java)) }
                    catch (failure: RuntimeException) { Toast.makeText(this@ImgurUploadsActivity, "スレを開けませんでした", Toast.LENGTH_SHORT).show() }
                }, enabled = !busy) { Text(thread) } }
            }
        }
    }
}
