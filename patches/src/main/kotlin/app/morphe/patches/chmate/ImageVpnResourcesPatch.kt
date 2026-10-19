package app.morphe.patches.chmate

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.filePathOption
import org.w3c.dom.Element

private object ImageVpnResources

/** Opt-in image VPN. Actual image-path and complete redirect/leak tests are still pending. */
@Suppress("unused")
val imageVpnResourcesPatch = resourcePatch(
    name = "画像取得専用VPN（実験用・検証中）",
    description = "必要な方だけ選択してください。GitHubからimage-stack.aarを保存: $OPTIONAL_ASSET_VPN_URL → 下のオプションで選択 → パッチ適用後にHaiagaru設定でVPNを有効化。AARは展開不要です。全経路の検証は未完了です。導入・ソース: $OPTIONAL_ASSET_GUIDE",
    default = false,
) {
    compatibleWith(chMateCompatibility)
    dependsOn(imageVpnTransportPatch)
    val libraryFile = filePathOption(
        key = "imageVpnLibraryFile", default = "", title = "ダウンロードしたVPNライブラリを選択",
        description = "GitHubからimage-stack.aarを保存して選択: $OPTIONAL_ASSET_VPN_URL AARは展開不要。$OPTIONAL_ASSET_GUIDE",
        allowedExtensions = listOf("aar"),
    )
    execute {
        val hashes = mapOf(
            "arm64-v8a" to "b58e5a0c803178e014641e835aea76e0674eac14e48d88e6b5a91fd9577ee450",
            "armeabi-v7a" to "43b2535cd19124f89300bcd120bbf6634d5bce4677306807b7b72712d59e6624",
            "x86" to "41cdbaac181c549d7785fc56c5d8db0ebce23470a68268c222cc8f1518440adb",
            "x86_64" to "8d1452d71b0140d93bda2e27532df1bea788be044f909a17024d6a0db2b2380f",
        )
        check(libraryFile.value.orEmpty().isNotBlank()) { "VPNライブラリを選択するか、この任意パッチのチェックを外してください。$OPTIONAL_ASSET_GUIDE" }
        val libraries = optionalLibraries(libraryFile.value.orEmpty(), hashes, "libgojni.so")
        libraries.forEach { (abi, bytes) ->
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
