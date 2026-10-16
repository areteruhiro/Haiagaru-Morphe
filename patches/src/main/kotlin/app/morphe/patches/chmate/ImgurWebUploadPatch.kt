package app.morphe.patches.chmate

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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
    execute {
        patchImgurPostingMenu(packageMetadata.versionName)
        patchImgurWebSessionUpload(packageMetadata.versionName)
        val (callback, row) = when (packageMetadata.versionName) {
            "0.8.10.191 dev" -> "Lo/onRewardedInterstitialAdLoadFailed\$2;" to "Lo/onRewardedInterstitialAdDisplayed;"
            "0.8.10.226 dev" -> "Lo/enB3\$write;" to "Lo/enB12;"
            "0.8.10.241" -> "Lo/TGq\$RemoteActionCompatParcelizer;" to "Lo/Pss;"
            "0.8.10.242 dev" -> "Lo/getServiceWithTimeout\$read;" to "Lo/hasResolution;"
            else -> error("Imgur native history profile is missing")
        }
        val method = mutableClassDefBy(callback).methods.single { method ->
            method.implementation?.instructions?.any {
                val ref = (it as? ReferenceInstruction)?.reference as? MethodReference
                ref?.definingClass == row && ref.name == "<init>"
            } == true
        }
        val instructions = method.implementation!!.instructions.toList()
        val index = instructions.indices.single {
            val ref = (instructions[it] as? ReferenceInstruction)?.reference as? MethodReference
            ref?.name == "add" && ref.definingClass in setOf("Ljava/util/AbstractCollection;", "Ljava/util/ArrayList;")
                && ref.parameterTypes == listOf("Ljava/lang/Object;") && ref.returnType == "Z"
        }
        val hook = "Lapp/morphe/extension/chmate/ImgurUploadHistory;->addNativeHistory(Ljava/util/Collection;Ljava/lang/Object;)Z"
        when (val call = instructions[index]) {
            is FiveRegisterInstruction -> method.replaceInstruction(index,
                "invoke-static {v${call.registerC}, v${call.registerD}}, $hook")
            is RegisterRangeInstruction -> method.replaceInstruction(index,
                "invoke-static/range {v${call.startRegister} .. v${call.startRegister + 1}}, $hook")
            else -> error("Imgur history append registers changed")
        }
    }
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
            for (activityName in listOf("ImgurWebLoginActivity", "ImgurUploadsActivity")) {
                application.appendChild(manifest.createElement("activity").apply {
                    val ns = "http://schemas.android.com/apk/res/android"
                    setAttributeNS(ns, "android:name", "app.morphe.extension.chmate.$activityName")
                    setAttributeNS(ns, "android:exported", "false")
                    setAttributeNS(ns, "android:theme", "@android:style/Theme.Material.Light.NoActionBar")
                })
            }
        }
    }
}
