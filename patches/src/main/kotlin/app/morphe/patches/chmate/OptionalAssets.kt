package app.morphe.patches.chmate

import app.morphe.patcher.patch.PatchException
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

internal const val OPTIONAL_ASSET_GUIDE = "https://github.com/areteruhiro/Haiagaru-Morphe/blob/dev/docs/optional-assets.md"
internal const val OPTIONAL_ASSET_EMOJI_URL = "https://raw.githubusercontent.com/areteruhiro/Haiagaru-Morphe/79759fb0ab0fa30a6755be13a53546b06d2cdf8d/patches/src/main/resources/chmate/emoji/NotoColorEmoji.ttf"
internal const val OPTIONAL_ASSET_IMAGE_NG_URL = "https://raw.githubusercontent.com/areteruhiro/Haiagaru-Morphe/dev/optional-assets/haiagaru-image-ng.zip"
internal const val OPTIONAL_ASSET_VPN_URL = "https://raw.githubusercontent.com/areteruhiro/Haiagaru-Morphe/79759fb0ab0fa30a6755be13a53546b06d2cdf8d/vpn/image-stack/prebuilt/image-stack.aar"

/** Read named entries without extracting archive paths onto disk. Verify before modifying the APK. */
internal fun optionalLibraries(path: String, names: Map<String, String>, library: String): Map<String, ByteArray> {
    val file = File(path)
    if (!file.isFile || !file.canRead()) throw PatchException("選択したファイルを読み込めません。Morpheで再選択してください: $path")
    try {
        return ZipFile(file).use { zip ->
            names.mapValues { (abi, expected) ->
                val candidates = zip.entries().asSequence().filter {
                    !it.isDirectory && (it.name == "$abi/$library" || it.name.endsWith("/$abi/$library"))
                }.toList()
                if (candidates.size != 1) throw PatchException("$abi/$library が1つ含まれる配布ファイルを選択してください。$OPTIONAL_ASSET_GUIDE")
                val entry = candidates.single()
                if (entry.size !in 1..(32L * 1024 * 1024)) throw PatchException("ライブラリのサイズが不正です: $abi")
                val bytes = zip.getInputStream(entry).use { it.readNBytes(32 * 1024 * 1024 + 1) }
                val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                if (hash != expected) throw PatchException("対応するライブラリではありません。案内の配布ファイルを選択してください: $abi / $OPTIONAL_ASSET_GUIDE")
                bytes
            }
        }
    } catch (e: PatchException) { throw e }
    catch (e: Exception) { throw PatchException("追加ライブラリを読み込めません。ZIP/AAR/APKを展開せず選択してください: ${e.message}") }
}
