package app.morphe.extension.chmate

import android.content.Context
import android.os.Build
import dev.carlsen.mega.Mega
import dev.carlsen.mega.MegaLogLevel
import dev.carlsen.mega.MegaLogListener
import dev.carlsen.mega.MegaLogger
import dev.carlsen.mega.model.Node
import dev.carlsen.mega.util.CancellationToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSource
import kotlinx.io.asSink
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID

/** File-streaming MEGA transport; archive size is limited by storage, not a byte array. */
@OptIn(kotlin.time.ExperimentalTime::class)
internal object HaiagaruMegaClient {
    private const val FOLDER = "Haiagaru"
    private const val PREFIX = "Haiagaru-sync-"

    class MegaFile(
        @JvmField val hash: String,
        @JvmField val name: String,
        @JvmField val createdTime: String,
        @JvmField val size: Long,
    )

    @JvmStatic
    fun login(context: Context, email: String, password: String, mfa: String) = runBlocking {
        requireSupportedAndroid()
        val mega = createQuietClient()
        try {
            mega.login(email, password, mfa.ifEmpty { null })
            val session = mega.dumpSession() ?: throw IOException("MEGAセッションを取得できませんでした")
            HaiagaruMegaSession.saveSession(context, session)
        } finally {
            closeClient(mega)
        }
    }

    @JvmStatic
    fun upload(context: Context, snapshot: File): String = runBlocking {
        requireSupportedAndroid()
        if (!snapshot.isFile || snapshot.length() <= 0) {
            throw IOException("バックアップのサイズが不正です")
        }
        withSession(context) { mega ->
            val folder = folder(mega, create = true)
                ?: throw IOException("MEGAのバックアップフォルダを作成できません")
            val timestamp = BackupTime.filename(System.currentTimeMillis())
            val identity = HaiagaruBackupSecrets.identity(context)
            val device = (Build.MANUFACTURER + "-" + Build.MODEL)
                .replace(Regex("[^A-Za-z0-9._-]"), "_").take(64).ifEmpty { "Android" }
            val options = context.getSharedPreferences("haiagaru.mega-sync.options", 0)
            val overwrite = options.getBoolean(MegaBackupDeletionPolicy.OVERWRITE, false)
            // Snapshot the opt-in before starting the transfer, not halfway through deletion.
            val permanent = MegaBackupDeletionPolicy.permanent(overwrite,
                options.getBoolean(MegaBackupDeletionPolicy.PERMANENT, false))
            val name = if (overwrite) "$PREFIX$device-$identity.zip"
                else "$PREFIX$device-$timestamp-${UUID.randomUUID().toString().take(8)}.zip"
            // Capture exact old nodes before upload. Never delete another installation's backup.
            val previous = if (overwrite) mega.getChildren(folder).filter { it.isFile && it.name == name } else emptyList()
            val uploaded = FileInputStream(snapshot).asSource().buffered().use { source ->
                CancellationToken().use { token -> mega.uploadFile(folder, name, snapshot.length(), source, token) }
            }
            if (uploaded.size != snapshot.length()) throw IOException("アップロード後のサイズが一致しません。旧バックアップは保持します")
            if (overwrite && previous.isNotEmpty()) {
                val verification = HaiagaruBackupArchive.temporary(context)
                try {
                    downloadTo(mega, uploaded, verification)
                    if (!digest(snapshot).contentEquals(digest(verification))) throw IOException("バックアップ検証に失敗しました。旧バックアップは保持します")
                    for (old in previous) {
                        if (MegaBackupDeletionPolicy.canRemove(overwrite, true, name,
                                old.name, uploaded.hash, old.hash)) mega.delete(old, destroy = permanent)
                    }
                } finally { verification.delete() }
            }
            name
        }
    }

    @JvmStatic
    fun latest(context: Context): MegaFile = runBlocking {
        requireSupportedAndroid()
        withSession(context) { mega ->
            val folder = folder(mega, create = false)
                ?: throw IOException("MEGAにバックアップがありません")
            val node = mega.getChildren(folder).filter { eligible(it) }
                .maxByOrNull { it.timestamp.toEpochMilliseconds() }
                ?: throw IOException("MEGAにバックアップがありません")
            describe(node)
        }
    }

    @JvmStatic
    fun list(context: Context): Array<MegaFile> = runBlocking {
        requireSupportedAndroid()
        withSession(context) { mega ->
            val folder = folder(mega, false) ?: throw IOException("MEGAにバックアップがありません")
            mega.getChildren(folder).filter { eligible(it) }
                .sortedByDescending { it.timestamp.toEpochMilliseconds() }.map { describe(it) }.toTypedArray()
        }
    }

    private fun eligible(node: Node) = node.isFile && node.name.startsWith(PREFIX)
        && (node.name.endsWith(".json") || node.name.endsWith(".zip")) && node.size > 0

    private fun describe(node: Node): MegaFile = MegaFile(node.hash, node.name,
        BackupTime.display(node.timestamp.toEpochMilliseconds()), node.size)

    @JvmStatic
    fun download(context: Context, file: MegaFile): File = runBlocking {
        requireSupportedAndroid()
        withSession(context) { mega ->
            val node = mega.getNodeByHash(file.hash)
                ?: throw IOException("MEGA上のバックアップが見つかりません")
            if (!eligible(node) || node.name != file.name || node.size != file.size) {
                throw IOException("MEGA上のバックアップが変更されました")
            }
            val output = HaiagaruBackupArchive.temporary(context)
            try {
                downloadTo(mega, node, output)
                output
            } catch (error: Throwable) {
                output.delete()
                throw error
            }
        }
    }

    private suspend fun downloadTo(mega: Mega, node: Node, output: File) {
        if (output.parentFile!!.usableSpace < node.size + 8L * 1024 * 1024) throw IOException("バックアップ取得用の空き容量が不足しています")
        FileOutputStream(output).asSink().buffered().use { sink ->
            CancellationToken().use { token -> mega.downloadFile(node, sink, token) }
        }
        if (output.length() != node.size) throw IOException("取得したバックアップのサイズが一致しません")
    }

    private fun digest(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest()
    }

    private suspend fun folder(mega: Mega, create: Boolean): Node? {
        val root = mega.getFileSystem().root ?: throw IOException("MEGAのファイル一覧を取得できません")
        return mega.getChildren(root).firstOrNull { it.isFolder && it.name == FOLDER }
            ?: if (create) mega.createDir(FOLDER, root) else null
    }

    private suspend fun <T> withSession(context: Context, action: suspend (Mega) -> T): T {
        val mega = createQuietClient()
        try {
            mega.fastLogin(HaiagaruMegaSession.readSession(context))
            return action(mega)
        } finally {
            closeClient(mega)
        }
    }

    private fun requireSupportedAndroid() {
        if (Build.VERSION.SDK_INT < 24) throw IOException("MEGA同期にはAndroid 7以降が必要です")
    }

    /** The upstream beta client prints request URLs (including session IDs). Replace its HTTP client before any call. */
    private fun createQuietClient(): Mega {
        val logger = MegaLogger().apply {
            minLogLevel = MegaLogLevel.ERROR
            addListener(object : MegaLogListener {
                override fun onLogMessage(level: MegaLogLevel, message: String, throwable: Throwable?) {}
            })
        }
        val mega = Mega(megaLogger = logger, userAgent = "Haiagaru-Morphe", maxConcurrentChunks = 2)
        val field = Mega::class.java.getDeclaredField("httpClient").apply { isAccessible = true }
        val previous = field.get(mega) as HttpClient
        val quiet = HttpClient(OkHttp) {
            expectSuccess = false
            install(UserAgent) { agent = "Haiagaru-Morphe" }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 30_000
            }
            install(ContentNegotiation) {
                json(Json { prettyPrint = true; isLenient = true; ignoreUnknownKeys = true })
            }
        }
        field.set(mega, quiet)
        previous.close()
        return mega
    }

    private suspend fun closeClient(mega: Mega) {
        try {
            mega.logout()
        } finally {
            val field = Mega::class.java.getDeclaredField("httpClient").apply { isAccessible = true }
            (field.get(mega) as? HttpClient)?.close()
        }
    }
}
