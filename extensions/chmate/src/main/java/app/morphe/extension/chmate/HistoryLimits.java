package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

/** Separate display, automatic deletion and search-history limits. Never deletes DAT directly. */
public final class HistoryLimits {
    private static final String PREFS = "io.github.areteruhiro.chmate.haiagaru.ui-config";
    private static volatile Context context;
    private HistoryLimits() { }
    static void initialize(Context value) { context = value.getApplicationContext(); }
    private static SharedPreferences prefs() { return context.getSharedPreferences(PREFS, 0); }
    private static SharedPreferences nativePrefs() { return PreferenceManager.getDefaultSharedPreferences(context); }
    private static int defaultDisplay() {
        try { return context.getPackageManager().getPackageInfo(context.getPackageName(), 0)
                .versionName.contains("242") ? 50 : 0; }
        catch (Exception ignored) { return 50; }
    }
    public static boolean unlimitedThreadHistory() {
        return context != null && prefs().getBoolean("unlimitedThreadHistory", false);
    }
    public static int getRecentThreadDisplayCount() {
        int value = context == null ? 50 : prefs().getInt("recentThreadDisplayCount", defaultDisplay());
        return value <= 0 ? Integer.MAX_VALUE : Math.min(value, 10000);
    }
    public static long getRecentThreadDisplayCountLong() { return getRecentThreadDisplayCount(); }
    public static ArrayList<?> limitRecentThreadRows(ArrayList<?> rows) {
        int limit = getRecentThreadDisplayCount();
        return rows == null || rows.size() <= limit ? rows : new ArrayList<>(rows.subList(0, limit));
    }
    /** Return false only before initialization, so the original writer remains the fallback. */
    public static boolean recordSearchHistory(String query) {
        if (context == null) return false;
        SharedPreferences nativePrefs = nativePrefs();
        if (!nativePrefs.getBoolean("saveSearchHistory", true) || query == null || query.trim().isEmpty()) return true;
        int limit = prefs().getInt("searchHistoryCount", 50);
        if (limit <= 0) limit = Integer.MAX_VALUE;
        ArrayList<String> rows = new ArrayList<>();
        rows.add(query);
        for (String previous : nativePrefs.getString("searchRecent", "").split("\n")) {
            if (rows.size() >= limit) break;
            if (previous.trim().isEmpty() || query.equalsIgnoreCase(previous)) continue;
            rows.add(previous);
        }
        StringBuilder value = new StringBuilder();
        for (String row : rows) value.append(row).append('\n');
        nativePrefs.edit().putString("searchRecent", value.toString()).apply();
        return true;
    }
    static Settings addSettings(LinearLayout layout, Activity activity) {
        initialize(activity);
        return new Settings(layout, activity);
    }
    static final class Settings {
        final Activity activity;
        final Switch unlimited;
        final EditText retained, displayed, searches;
        final boolean oldUnlimited;
        final int oldRetained, oldDisplayed, oldSearches;
        Settings(LinearLayout layout, Activity activity) {
            this.activity = activity;
            oldUnlimited = unlimitedThreadHistory();
            oldRetained = nativePrefs().getInt("autoDelete", 300);
            oldDisplayed = prefs().getInt("recentThreadDisplayCount", defaultDisplay());
            oldSearches = prefs().getInt("searchHistoryCount", 50);
            unlimited = new Switch(activity);
            unlimited.setText("閲覧スレ履歴を無制限に保存（自動削除しない）");
            unlimited.setChecked(oldUnlimited);
            layout.addView(unlimited);
            retained = field(layout, "閲覧スレ履歴の保存数（1〜10000）", Math.max(1, Math.min(oldRetained, 10000)));
            retained.setEnabled(!oldUnlimited);
            unlimited.setOnCheckedChangeListener((button, checked) -> retained.setEnabled(!checked));
            displayed = field(layout, "最近見たスレの表示数（0で無制限）", oldDisplayed);
            searches = field(layout, "検索履歴の保存数（0で無制限）", oldSearches);
            TextView description = new TextView(activity);
            description.setText("閲覧スレ履歴の無制限は、古い履歴・DATの件数による自動削除を止めます。"
                    + "手動削除は維持します。有限の保存数はChMate標準の自動整理タイミングで適用されます。"
                    + "検索履歴はChMateの「検索履歴を保存」をオフにすると保存しません。"
                    + "板の履歴には元々件数制限がありません。無制限では容量・読み込み負荷が増える場合があります。"
                    + "設定変更時はアプリを再起動します。削除済みの履歴は復元できません。");
            description.setTextSize(13);
            layout.addView(description);
        }
        private EditText field(LinearLayout layout, String label, int value) {
            TextView title = new TextView(activity); title.setText(label); layout.addView(title);
            EditText field = new EditText(activity); field.setInputType(InputType.TYPE_CLASS_NUMBER);
            field.setText(Integer.toString(value)); layout.addView(field); return field;
        }
        private int number(EditText field) {
            try { return Integer.parseInt(field.getText().toString().trim()); }
            catch (RuntimeException ignored) { return -1; }
        }
        boolean validate() {
            int retention = number(retained), display = number(displayed), search = number(searches);
            boolean valid = (unlimited.isChecked() || retention >= 1 && retention <= 10000)
                    && display >= 0 && display <= 10000 && search >= 0 && search <= 10000;
            if (!valid) Toast.makeText(activity, "履歴件数は表示された範囲の数値で入力してください。設定は保存されませんでした。", Toast.LENGTH_LONG).show();
            return valid;
        }
        boolean save() {
            int retention = unlimited.isChecked() ? oldRetained : number(retained);
            int display = number(displayed), search = number(searches);
            prefs().edit().putBoolean("unlimitedThreadHistory", unlimited.isChecked())
                    .putInt("recentThreadDisplayCount", display).putInt("searchHistoryCount", search).commit();
            if (retention != oldRetained) nativePrefs().edit().putInt("autoDelete", retention).commit();
            return oldUnlimited != unlimited.isChecked() || oldRetained != retention
                    || oldDisplayed != display || oldSearches != search;
        }
    }
}
