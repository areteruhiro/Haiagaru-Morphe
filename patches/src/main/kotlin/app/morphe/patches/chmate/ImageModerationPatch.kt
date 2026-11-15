package app.morphe.patches.chmate

import app.morphe.patcher.patch.filePathOption
import app.morphe.patcher.patch.resourcePatch
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import org.w3c.dom.Element

private const val modelPath = "assets/haiagaru/image-moderation/model.onnx"
private val moderationHashes = mapOf(
        modelPath to "8c28c49d9075f3ad15ebdc2961f02d5b3f99be944815b848b49c9f0e6f3fb689",
        "lib/arm64-v8a/libonnxruntime.so" to "e40f09d07dc53726b8bfbf48a7907673b8f86718a057655a62790a39874a7302",
        "lib/arm64-v8a/libonnxruntime4j_jni.so" to "2cc3f16ab527959a81b572adebcfb71a25ea4b8547c9b92d705b423e9c822d39",
        "lib/armeabi-v7a/libonnxruntime.so" to "57048b8d54896d16355ee367bfc129c5925468ae503b681b8d0cd49ceefa468e",
        "lib/armeabi-v7a/libonnxruntime4j_jni.so" to "d977cebd40e780d1ece419c182e27610e74e04ad3f0244e9acef887f0ffb0247",
        "lib/x86/libonnxruntime.so" to "213d91ebb0cfd511c18c0057c69145de0abc6bdc9c63429bf04dcdeaf3fd861a",
        "lib/x86/libonnxruntime4j_jni.so" to "375c7e60e5f11ac35f38b24682baa8e82d63d104c9bb3868db388118901dc45f",
        "lib/x86_64/libonnxruntime.so" to "972c17c056eaae946a415d9efdd8018b729639974df075e495f0092441478fb7",
        "lib/x86_64/libonnxruntime4j_jni.so" to "b8d1b251b7d5606cf5caa7db9738353c08efc4090ac90ca2aaab449bc263a10b",
        "chmate191/arm64-v8a/libtensorflowlite_jni.so" to "430a5cc57c285e9f7a8dede904eb998ad50c634e592baf7f06f8a896b2dea557",
        "chmate191/armeabi-v7a/libtensorflowlite_jni.so" to "ba8c1f290d797820695231f86a8de16c74d1fa10595ea33d9397791f0f93d52b",
        "chmate191/x86/libtensorflowlite_jni.so" to "1a8bad7f404050f259b9224e572106beb1326bc1c66c9856b4ecc58f5358df74",
        "chmate191/x86_64/libtensorflowlite_jni.so" to "a7597671e6685259cd27959040959786c2c5e8f5dfd8303746554b3644315f5b",
)

@Suppress("unused")
val imageModerationPatch = resourcePatch(
    name = "成人向け・グロ画像の自動検出",
    description = "専用ZIPを展開せず選択します。導入手順: $OPTIONAL_ASSET_GUIDE",
    default = false,
) {
    compatibleWith(chMateCompatibility)
    dependsOn(haiagaruPatch)
    val bundle = filePathOption(
        key = "imageModerationFile", default = "", title = "haiagaru-image-moderation.zip",
        description = "モデルと実行ライブラリをまとめたZIPを選択してください。",
        allowedExtensions = listOf("zip"),
    )
    execute {
        check(bundle.value.orEmpty().isNotBlank()) { "画像検出用ZIPを選ぶか、任意パッチのチェックを外してください。" }
        ZipFile(File(bundle.value.orEmpty())).use { zip ->
            val items = zip.entries().asSequence().filterNot { it.isDirectory }.groupBy { it.name }
            val notices = setOf("assets/haiagaru/image-moderation/ONNX-LICENSE.txt",
                "assets/haiagaru/image-moderation/ONNX-ThirdPartyNotices.txt",
                "assets/haiagaru/image-moderation/MODEL-NOTICE.txt",
                "assets/haiagaru/image-moderation/SWIFTFORMER-LICENSE.txt")
            val files = moderationHashes.keys + notices
            check(items.keys == files && files.all { items.getValue(it).size == 1 }) { "画像検出用ZIPの内容が一致しません。" }
            for (name in files) {
                val entry = items.getValue(name).single()
                check(entry.size in 1..(32L * 1024 * 1024)) { "画像検出用ZIPのサイズが不正です: $name" }
                val bytes = zip.getInputStream(entry).use { it.readNBytes(entry.size.toInt() + 1) }
                check(bytes.size.toLong() == entry.size) { "画像検出用ZIPのサイズが不正です: $name" }
                moderationHashes[name]?.let { expected ->
                    val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
                    check(hash == expected) { "画像検出用ZIPのファイルが一致しません: $name" }
                }
                val legacy = name.startsWith("chmate191/")
                if (legacy && packageMetadata.versionName != "0.8.10.191 dev") continue
                val target = if (legacy) "lib/" + name.removePrefix("chmate191/") else name
                get(target).apply {
                    check(legacy || !exists()) { "画像検出用ファイルが元APKに存在します: $target" }
                    parentFile.mkdirs()
                    writeBytes(bytes)
                }
            }
            if (packageMetadata.versionName == "0.8.10.191 dev")
                get("assets/haiagaru/image-ng-runtime-fixed").apply { parentFile.mkdirs(); writeText("1") }
        }
        document("AndroidManifest.xml").use { manifest ->
            (manifest.getElementsByTagName("application").item(0) as Element).setAttribute("android:extractNativeLibs", "true")
        }
    }
}
