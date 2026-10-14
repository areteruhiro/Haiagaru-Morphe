package android.content;
import android.content.res.AssetManager;
public class Context {
    public static final int MODE_PRIVATE = 0;
    public boolean installed = true;
    public Context getApplicationContext() { return this; }
    public AssetManager getAssets() { return new AssetManager(installed); }
    public SharedPreferences getSharedPreferences(String name, int mode) {
        if (!"io.github.areteruhiro.chmate.haiagaru.ui-config".equals(name)) throw new AssertionError("Wrong prefs");
        return new SharedPreferences();
    }
}
