package app.morphe.patches.chmate

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element

private val imgurWebCompatibility = Compatibility(
    name = "ChMate",
    packageName = "jp.co.airfront.android.a2chMate",
    apkFileType = ApkFileType.APK,
    appIconColor = 0x607D8B,
    signatures = setOf("7dd84d97df4666fbc8188b8d6167ce59314636997f0edae82d685fffda4059d2"),
    targets = listOf(
        AppTarget(version = "0.8.10.191 dev", minSdk = 21),
        AppTarget(version = "0.8.10.226 dev", minSdk = 23),
        AppTarget(version = "0.8.10.241", minSdk = 23),
        AppTarget(version = "0.8.10.242 dev", minSdk = 23),
    ),
)

private val imgurWebUploadBytecodePatch = bytecodePatch {
    dependsOn(haiagaruPatch)
    execute { patchImgurWebSessionUpload(packageMetadata.versionName) }
}

/** Deliberately opt-in while real account upload and service compatibility are being tested. */
@Suppress("unused")
val imgurWebUploadPatch = resourcePatch(
    name = "Imgurログインアップロード（開発中）",
    description = "アプリ内でImgurにログインし、既存の画像アップロードからアカウントへ送信します。初期状態は未選択。導入後もログインと有効化が必要です。非公式Web方式で動作保証はなく、利用規約・利用制限が適用されます。失敗時に匿名送信へ切り替えません。",
    default = false,
) {
    compatibleWith(imgurWebCompatibility)
    dependsOn(imgurWebUploadBytecodePatch)
    execute {
        get("assets").resolve("haiagaru/imgur-web/installed").apply {
            parentFile.mkdirs()
            writeText("1")
        }
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.getElementsByTagName("application").item(0) as Element
            application.appendChild(manifest.createElement("activity").apply {
                val ns = "http://schemas.android.com/apk/res/android"
                setAttributeNS(ns, "android:name", "app.morphe.extension.chmate.ImgurWebLoginActivity")
                setAttributeNS(ns, "android:exported", "false")
                setAttributeNS(ns, "android:theme", "@android:style/Theme.Material.Light.NoActionBar")
            })
        }
    }
}
