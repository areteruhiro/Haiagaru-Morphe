package app.morphe.patches.chmate

import app.morphe.patcher.patch.resourcePatch
import java.security.MessageDigest
import org.w3c.dom.Element

private object ImageVpnResources

/** Opt-in image VPN. Actual image-path and complete redirect/leak tests are still pending. */
@Suppress("unused")
val imageVpnResourcesPatch = resourcePatch(
    name = "画像取得専用VPN（実験用・検証中）",
    description = "ChMate単体のVPN Gate画像取得機能を追加します。初期状態は未選択。導入後もHaiagaru設定で有効化が必要です。対象の画像のみVPN経由で取得します。対象画像の表示・全経路の通信漏れ検証は未完了です。VPN運営者に接続元IPが見えることや、固有URLによる識別は防げません。",
    default = false,
) {
    compatibleWith(chMateCompatibility)
    dependsOn(imageVpnTransportPatch)
    execute {
        val hashes = mapOf(
            "arm64-v8a" to "b58e5a0c803178e014641e835aea76e0674eac14e48d88e6b5a91fd9577ee450",
            "armeabi-v7a" to "43b2535cd19124f89300bcd120bbf6634d5bce4677306807b7b72712d59e6624",
            "x86" to "41cdbaac181c549d7785fc56c5d8db0ebce23470a68268c222cc8f1518440adb",
            "x86_64" to "8d1452d71b0140d93bda2e27532df1bea788be044f909a17024d6a0db2b2380f",
        )
        hashes.forEach { (abi, expected) ->
            val path = "/chmate/image-vpn/jni/$abi/libgojni.so"
            val bytes = requireNotNull(ImageVpnResources::class.java.getResourceAsStream(path)) {
                "Missing image VPN native library: $abi"
            }.use { it.readBytes() }
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            check(hash == expected) { "Image VPN native library checksum mismatch: $abi" }
            get("lib").resolve("$abi/libgojni.so").apply {
                check(!exists()) { "Image VPN JNI library collides with the input APK: $abi" }
                parentFile.mkdirs()
                writeBytes(bytes)
            }
        }
        val notices = listOf("open-sstp/LICENSE", "image-stack/NOTICE.md") +
            listOf("go", "gvisor", "btree", "x-mobile", "x-sys", "x-time").map { "image-stack/$it/LICENSE" }
        notices.forEach { notice ->
            val path = "/chmate/image-vpn/licenses/$notice"
            val bytes = requireNotNull(ImageVpnResources::class.java.getResourceAsStream(path)).use { it.readBytes() }
            get("assets").resolve("haiagaru/image-vpn/licenses/$notice").apply {
                parentFile.mkdirs()
                writeBytes(bytes)
            }
        }
        get("assets").resolve("haiagaru/image-vpn/installed").apply {
            parentFile.mkdirs()
            writeText("1")
        }
        // Native ELF LOAD segments were verified with 16KB alignment. Extraction also avoids
        // relying on a manager's ZIP native-entry alignment for Android 15+ devices.
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as Element
            application.setAttribute("android:extractNativeLibs", "true")
        }
    }
}
