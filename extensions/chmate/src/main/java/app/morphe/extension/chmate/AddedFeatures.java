package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A version-scoped index of the same preferences used by each feature's settings. */
final class AddedFeatures {
    static void show(Activity activity) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        TextView explanation = new TextView(activity);
        explanation.setText("stable 1.24.1以降に追加・拡張した機能です。各設定画面と同じ値を変更します。スレの表示変更は開き直すと反映されます。");
        content.addView(explanation);
        SharedPreferences main = SettingsProfiles.prefs(activity);
        toggle(content, activity, main, "設定文字列をタップして設定を開く", "settingsOptionLinksEnabled", true, null);
        toggle(content, activity, main, "外部板をサイト別にまとめる", ExternalBoardGroups.KEY, false, null);
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
                "スレ一覧に画像・動画サムネイルを表示", ThreadListThumbnailSettings.ENABLE, false,
                ThreadListThumbnail::refresh);
        toggle(content, activity, main, "記者ID・スレ名の期間指定検索", "reporterSearchEnabled", true, null);
        toggle(content, activity, main, "エッヂ・kenmo.orgの記者IDをレスメニューに表示", EdgeReporterOptions.MENU, true, null);
        toggle(content, activity, main, "エッヂ・kenmo.orgの記者IDをスレ一覧に表示", EdgeReporterOptions.BOARD, true, null);
        toggle(content, activity, RegisteredImageNg.prefs(activity),
                "登録画像に一致するレスをあぼーん", RegisteredImageNg.ENABLE, false,
                RegisteredImageNg::settingsChanged);
        toggle(content, activity, main, "コピペ省略されたレスを透明あぼーん", ChainAboneVisibility.COPY_PASTE_KEY, false, null);
        toggle(content, activity, StringReplacement.prefs(activity),
                "ReplaceTxtによる文字列置換（正規表現対応）", "enabled", false,
                () -> StringReplacement.setEnabled(activity, StringReplacement.prefs(activity).getBoolean("enabled", false)));
        toggle(content, activity, RefreshTargets.prefs(activity),
                "履歴ポップアップの対象タブ", RefreshTargetHistory.VISIBLE_KEY, false, null);
        toggle(content, activity, RefreshTargets.prefs(activity),
                "投稿後の自動監視", "watchAfterPosting", false, null);
        toggle(content, activity, main, "動画からサムネイルを生成", "videoFrameThumbnailsEnabled", true, null);
        toggle(content, activity, main, "専用動画ビューアの横画面で没入表示", "videoLandscapeImmersive", true, null);
        toggle(content, activity, main, "既読スレ削除を更新ボタン長押しで開く", DeleteReadThreads.LONG_PRESS_KEY, true, null);
        addIpNetwork(content, activity, main, "IP表示スレへの投稿前確認（Wi-Fi）", 1);
        addIpNetwork(content, activity, main, "IP表示スレへの投稿前確認（モバイル回線）", 2);
        Button images = new Button(activity);
        images.setText("登録画像・判定レベル・一致枚数を設定");
        images.setOnClickListener(view -> activity.startActivity(new Intent(activity, RegisteredImageNgSettingsActivity.class)));
        content.addView(images);
        TextView fixes = new TextView(activity);
        fixes.setText("同期間の修正には、外部板のURL・画像・板名・一覧件数、監視通知の移動と期限指定、サムネイルの重なり、投稿本文の文字処理、共有NGの通信・誤判定、カスタムアイコンの大きさ、画像・動画のページ移動と保存、dat共有のクラッシュ対策が含まれます。");
        content.addView(fixes);
        new HaiagaruDialog.Builder(activity).setTitle("1.24.1以降の追加機能")
                .setView(content).setPositiveButton("閉じる", null).show();
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

    private AddedFeatures() {}
}
