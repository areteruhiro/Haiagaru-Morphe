package app.morphe.extension.chmate

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Separate destinations share the validated backup backend. */
class BackupSettingsActivity : HaiagaruComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mega = intent.getBooleanExtra("mega", false)
        HaiagaruUi.showForm(this, if (mega) "MEGAバックアップ・同期" else "ローカルバックアップ",
            HaiagaruMegaSync.settingsForm(this, mega))
    }

    companion object {
        @JvmStatic fun open(activity: Activity, mega: Boolean) {
            activity.startActivity(Intent(activity, BackupSettingsActivity::class.java).putExtra("mega", mega))
        }
    }
}
