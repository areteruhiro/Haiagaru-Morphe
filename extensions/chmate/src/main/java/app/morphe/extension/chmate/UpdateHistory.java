package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Release notes generated from CHANGELOG.md as part of every release build. */
final class UpdateHistory {
    static void show(Activity activity) {
        Context context = SettingsTabUi.context(activity);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);

        TextView version = new TextView(context);
        version.setText("Haiagaru " + BuildConfig.HAIAGARU_VERSION);
        content.addView(version);

        TextView settingsHeading = new TextView(context);
        settingsHeading.setText("機能のオン・オフ");
        content.addView(settingsHeading);
        addFeatureSettings(content, activity);

        TextView historyHeading = new TextView(context);
        historyHeading.setText("リリース履歴");
        content.addView(historyHeading);
        TextView history = new TextView(context);
        history.setText(readBundledHistory(context));
        history.setTextIsSelectable(true);
        content.addView(history);

        Button releases = new Button(context);
        releases.setText("GitHubで更新履歴を開く");
        releases.setOnClickListener(view -> activity.startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/areteruhiro/Haiagaru-Morphe/releases"))));
        content.addView(releases);

        new HaiagaruDialog.Builder(activity).setTitle("更新履歴")
                .setView(content).setPositiveButton("閉じる", null).show();
    }

    private static void addFeatureSettings(LinearLayout content, Activity activity) {
        SharedPreferences main = SettingsProfiles.prefs(activity);
        toggle(content, activity, main, "設定文字列をタップして設定を開く",
                "settingsOptionLinksEnabled", true, null);
        toggle(content, activity, main, "外部板をサイト別にまとめる",
                ExternalBoardGroups.KEY, false, null);
        SiteBoardGateway gateway = SiteBoardGateway.get(activity);
        for (String kind : SiteBoardMenuProtocol.KINDS) {
            LiveSettings.Toggle site = new LiveSettings.Toggle(activity);
            site.setText(SiteBoardMenuProtocol.title(kind) + "の閲覧・板一覧");
            site.setChecked(gateway.siteEnabled(kind));
            site.setOnCheckedChangeListener((view, on) ->
                    gateway.prefs.edit().putBoolean(SiteBoardGateway.siteFlag(kind), on).apply());
            content.addView(site);
        }
        toggle(content, activity, ThreadListThumbnailSettings.prefs(activity),
                "スレ一覧に画像・動画サムネイルを表示", ThreadListThumbnailSettings.ENABLE,
                false, ThreadListThumbnail::refresh);
        toggle(content, activity, main, "記者ID・スレ名の期間指定検索",
                "reporterSearchEnabled", true, null);
        toggle(content, activity, main, "エッヂ・kenmo.orgの記者IDをレスメニューに表示",
                EdgeReporterOptions.MENU, true, null);
        toggle(content, activity, main, "エッヂ・kenmo.orgの記者IDをスレ一覧に表示",
                EdgeReporterOptions.BOARD, true, null);
        toggle(content, activity, RegisteredImageNg.prefs(activity),
                "登録画像に一致するレスをあぼーん", RegisteredImageNg.ENABLE, false,
                RegisteredImageNg::settingsChanged);
        toggle(content, activity, main, "コピペ省略されたレスを透明あぼーん",
                ChainAboneVisibility.COPY_PASTE_KEY, false, null);
        toggle(content, activity, StringReplacement.prefs(activity),
                "ReplaceTxtによる文字列置換（正規表現対応）", "enabled", false,
                () -> StringReplacement.setEnabled(activity,
                        StringReplacement.prefs(activity).getBoolean("enabled", false)));
        toggle(content, activity, RefreshTargets.prefs(activity),
                "履歴ポップアップの対象タブ", RefreshTargetHistory.VISIBLE_KEY, false, null);
        toggle(content, activity, RefreshTargets.prefs(activity),
                "投稿後の自動監視", "watchAfterPosting", false, null);
        toggle(content, activity, main, "動画からサムネイルを生成",
                "videoFrameThumbnailsEnabled", true, null);
        toggle(content, activity, main, "専用動画ビューアの横画面で没入表示",
                "videoLandscapeImmersive", true, null);
        toggle(content, activity, main, "既読スレ削除を更新ボタン長押しで開く",
                DeleteReadThreads.LONG_PRESS_KEY, true, null);
        addIpNetwork(content, activity, main, "IP表示スレへの投稿前確認（Wi-Fi）", 1);
        addIpNetwork(content, activity, main, "IP表示スレへの投稿前確認（モバイル回線）", 2);

        Button images = new Button(activity);
        images.setText("登録画像・判定レベル・一致枚数を設定");
        images.setOnClickListener(view -> activity.startActivity(
                new Intent(activity, RegisteredImageNgSettingsActivity.class)));
        content.addView(images);
    }

    private static void toggle(LinearLayout content, Activity activity, SharedPreferences prefs,
                               String label, String key, boolean initial, Runnable effect) {
        LiveSettings.Toggle toggle = new LiveSettings.Toggle(activity);
        toggle.setText(label);
        toggle.setChecked(prefs.getBoolean(key, initial));
        toggle.setOnCheckedChangeListener((view, on) -> {
            prefs.edit().putBoolean(key, on).apply();
            if (effect != null) effect.run();
            if (prefs == SettingsProfiles.prefs(activity)) SettingsEffects.changed(activity, key, on);
        });
        content.addView(toggle);
    }

    private static void addIpNetwork(LinearLayout content, Activity activity, SharedPreferences prefs,
                                     String label, int bit) {
        LiveSettings.Toggle toggle = new LiveSettings.Toggle(activity);
        toggle.setText(label);
        toggle.setChecked((prefs.getInt(PostingIpWarning.KEY, 0) & bit) != 0);
        toggle.setOnCheckedChangeListener((view, on) -> {
            int flags = prefs.getInt(PostingIpWarning.KEY, 0);
            prefs.edit().putInt(PostingIpWarning.KEY, on ? flags | bit : flags & ~bit).apply();
        });
        content.addView(toggle);
    }

    static String readBundledHistory(Context context) {
        try {
            int id = context.getResources().getIdentifier(
                    "haiagaru_update_history", "raw", "app.morphe.extension.chmate");
            if (id == 0) throw new IllegalStateException("更新履歴リソースがありません");
            try (InputStream input = context.getResources().openRawResource(id);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                for (int read; (read = input.read(buffer)) >= 0; ) output.write(buffer, 0, read);
                return output.toString(StandardCharsets.UTF_8.name()).trim();
            }
        } catch (Exception error) {
            return "更新履歴を読み込めませんでした。GitHubの更新履歴を確認してください。";
        }
    }

    private UpdateHistory() {}
}
