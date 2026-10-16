package app.morphe.patches.chmate

import app.morphe.patcher.patch.resourcePatch

@Suppress("unused")
val megaSyncPatch = resourcePatch(
    name = "MEGAバックアップ・同期",
    description = "MEGAアカウントへのバックアップ・復元・起動時同期を追加します。注意：Android 7.0以降が必要です。初期状態は未選択で、自動同期も初期OFFです。このパッチを選択しなくてもローカルバックアップ・復元は利用できます。",
    default = false,
) {
    compatibleWith(chMateCompatibility)
    dependsOn(haiagaruPatch)
    execute {
        get("assets").resolve("haiagaru/mega-sync/installed").apply {
            parentFile.mkdirs()
            writeText("1")
        }
    }
}
