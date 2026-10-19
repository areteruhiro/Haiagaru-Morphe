package app.morphe.extension.chmate

import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import java.io.File

/** Private, separately numbered UI resources avoid replacing ChMate's resource table. */
open class HaiagaruComposeActivity : ComponentActivity() {
    private var uiResources: Resources? = null
    override fun getResources(): Resources = uiResources ?: super.getResources()

    override fun onCreate(savedInstanceState: Bundle?) {
        val base = super.getResources()
        val resourceFile = File(codeCacheDir, "haiagaru-ui-${packageManager.getPackageInfo(packageName, 0).lastUpdateTime}.apk")
        if (!resourceFile.isFile) {
            val temporary = File(resourceFile.path + ".tmp")
            assets.open("haiagaru-ui-res.apk").use { input -> temporary.outputStream().use(input::copyTo) }
            check(temporary.renameTo(resourceFile)) { "Unable to install UI resources" }
        }
        uiResources = if (Build.VERSION.SDK_INT >= 30) {
            Resources(base.assets, base.displayMetrics, base.configuration).apply {
                val loader = android.content.res.loader.ResourcesLoader()
                ParcelFileDescriptor.open(resourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use {
                    loader.addProvider(android.content.res.loader.ResourcesProvider.loadFromApk(it))
                }
                addLoaders(loader)
            }
        } else {
            if (Build.VERSION.SDK_INT >= 28) org.lsposed.hiddenapibypass.HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/content/res/AssetManager;")
            @Suppress("DEPRECATION")
            val manager = AssetManager::class.java.getDeclaredConstructor().newInstance()
            val add = AssetManager::class.java.getMethod("addAssetPath", String::class.java)
            add.invoke(manager, applicationInfo.sourceDir)
            applicationInfo.splitSourceDirs?.forEach { add.invoke(manager, it) }
            check((add.invoke(manager, resourceFile.path) as Int) != 0)
            Resources(manager, base.displayMetrics, base.configuration)
        }
        super.onCreate(savedInstanceState)
    }
}
