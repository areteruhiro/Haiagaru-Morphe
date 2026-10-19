package app.morphe.extension.chmate;

import android.content.Context;

/** Read the feature preference at its use site. Settings live with their owning feature. */
final class FeatureSwitches {
    static boolean enabled(String key, boolean initial) {
        Context context = Haiagaru.applicationContextForExtension();
        return context == null ? initial : context.getSharedPreferences(WacchoiSearchOptions.PREFS, 0).getBoolean(key, initial);
    }
    private FeatureSwitches() { }
}
