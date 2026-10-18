package app.morphe.extension.chmate;

import android.app.*;
import android.content.*;
import android.widget.*;

/** Writes actual feature preferences rather than presentation-only aliases. */
final class FeatureSwitches {
    static boolean enabled(String key,boolean initial){Context c=Haiagaru.applicationContextForExtension();return c==null?initial:c.getSharedPreferences(WacchoiSearchOptions.PREFS,0).getBoolean(key,initial);}
    private static final String[][] OPTIONS={
        {"閲覧・一覧","directBodySelection","本文の長押しコピー","false"},
        {"閲覧・一覧","directCopyAnchors","コピー中のアンカー操作優先","true"},
        {"閲覧・一覧","groupBrowsingHistoryByBoard","閲覧履歴を板ごとに整理","true"},
        {"閲覧・一覧","readThreadsFirst","既読スレを上に","false"},
        {"閲覧・一覧",HiddenDroppedThreads.KEY,"DAT落ちスレを非表示","false"},
        {"閲覧・一覧","keepQuickFilterPosition","フィルタ切替時の位置維持","false"},
        {"閲覧・一覧","compactQuickFilters","上部フィルタをコンパクト表示","false"},
        {"閲覧・一覧","hideNgThreadGroup","NGグループを非表示","false"},
        {"閲覧・一覧",DeleteReadThreads.LONG_PRESS_KEY,"更新ボタン長押しの既読スレ削除","true"},
        {"閲覧・一覧",EdgeReporterOptions.MENU,EdgeReporterOptions.MENU_LABEL,"true"},
        {"閲覧・一覧",EdgeReporterOptions.BOARD,EdgeReporterOptions.BOARD_LABEL,"true"},
        {"閲覧・一覧","popularReplyPopup","人気レスポップアップ","false"},
        {"保存・投稿","responseArchiveEnabled","レス保存機能（保存済みデータは保持）","true"},
        {"保存・投稿","threadCommandsEnabled","スレ立てコマンド候補","true"},
        {"検索","wacchoiSearchEnabled","ワッチョイ上／下4桁検索","true"},
        {"検索","wacchoiBackgroundSearch","通知付きバックグラウンド検索","false"},
        {"検索","wacchoiSearchHistory","4桁検索の条件・結果保存","true"},
        {"検索",RelatedThreads.KEY,"エッヂの関連・次スレ候補","false"},
        {"画像・動画","videoFrameThumbnailsEnabled","動画フレームからのサムネイル生成","true"},
        {"画像・動画","videoPreviewFour","動画サムネイルの時系列分割","false"},
        {"画像・動画","videoPreviewWifiOnly","時系列分割の取得をWi-Fiのみに制限","true"},
        {"画像・動画","imageViewerRotationButton","画像回転ボタン","true"},
        {"画像・動画","videoViewerEnabled","内部動画ビューア（パッチ導入時のみ）","true"},
        {"画像・動画","videoYoutubeEnabled","YouTubeの内部表示","true"},
        {"画像・動画","videoSwipeBrightness","動画のスワイプ明るさ調整","false"},
        {"画像・動画","videoSwipeVolume","動画のスワイプ音量調整","false"},
        {"画像・動画","videoDoubleTapSeek","動画のダブルタップシーク","false"},
        {"Xプレビュー","xPostPreviewEnabled","投稿プレビュー","false"},
        {"Xプレビュー","xPostPreviewFullText","投稿の全文表示","true"},
        {"Xプレビュー","xPostPreviewCollapsible","プレビュー折りたたみ","false"},
        {"Xプレビュー","xPostPreviewActions","返信・リポスト・いいね等の操作欄","false"},
        {"Xプレビュー","xPostPreviewCard","カード表示","false"},
        {"Xプレビュー","xPostPreviewBrowser","投稿をアプリ内Web画面で開く","true"},
        {"通信","automaticDat","過去ログの自動DAT取得","true"},
        {"通信","forceHttps","HTTP通信をHTTPSへ切替","false"}
    };
    static void addSettings(LinearLayout root,Activity a){Button button=new Button(a);button.setText("追加機能のオン／オフ一覧");root.addView(button);button.setOnClickListener(v->show(a));}
    private static void show(Activity a){
        LinearLayout panel=new LinearLayout(a);panel.setOrientation(1);TextView note=new TextView(a);note.setText("各機能の設定画面と同じ値を変更します。親機能がオフなら子設定は動作しません。オフでも保存データは削除せず、画面を開き直して反映します。動画の任意パッチ等は導入が必要です。安全対策・クラッシュ修正は無効化しません。数値・板別指定・バックアップ等の詳細は各カテゴリから変更してください。実行中の取得は完了まで続く場合があります。");panel.addView(note);
        SharedPreferences prefs=a.getSharedPreferences(WacchoiSearchOptions.PREFS,0);String category="";
        for(String[] option:OPTIONS){if(!category.equals(option[0])){category=option[0];TextView title=new TextView(a);title.setText(category);title.setTextSize(18);panel.addView(title);}
            Switch s=new Switch(a);s.setText(option[2]);s.setChecked(prefs.getBoolean(option[1],Boolean.parseBoolean(option[3])));panel.addView(s);s.setOnCheckedChangeListener((v,on)->{
                prefs.edit().putBoolean(option[1],on).apply();
                SettingsEffects.changed(a,option[1],on);
            });
        }
        ResponseMenuSettings.addSettings(panel,a);ToolbarIcons.addSettingsButton(panel,a);
        ScrollView scroll=new ScrollView(a);scroll.addView(panel);new AlertDialog.Builder(a).setTitle("追加機能のオン／オフ一覧").setView(scroll).setPositiveButton("閉じる",null).show();
    }
    private FeatureSwitches(){}
}
