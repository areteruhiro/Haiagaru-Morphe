package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/** Hands an explicit draft to stock ChMate's exported editor. Never reads cookies or sends a post. */
public final class StockChmatePosting {
    private static final String PACKAGE = "jp.co.airfront.android.a2chMate";
    private static final String EDITOR = "jp.syoboi.a2chMate.activity.ResEditActivity";
    private static final int MENU = 0x48690062;
    private static final Map<Activity, Draft> DRAFTS = new WeakHashMap<>();
    private static final Map<MenuItem, WeakReference<Activity>> MENUS = new WeakHashMap<>();
    private static final class Draft {
        final String target;
        final WeakReference<Object> owner;
        final WeakReference<EditText> body;
        Draft(Object owner, EditText body, String target) {
            this.owner = new WeakReference<>(owner); this.body = new WeakReference<>(body); this.target = target;
        }
        String subject() throws Exception {
            if (!StockChmatePostingRules.newThread(target)) return null;
            return (String) owner.get().getClass().getMethod("haiagaruStockPostTitle").invoke(owner.get());
        }
    }

    static void target(Object owner, String address, EditText body) {
        Activity activity = ExternalBoardAuth.activity(owner);
        if (activity == null || body == null) return;
        String target = StockChmatePostingRules.target(address);
        if (target == null || PACKAGE.equals(activity.getPackageName())) DRAFTS.remove(activity);
        else DRAFTS.put(activity, new Draft(owner, body, target));
        activity.invalidateOptionsMenu();
    }

    private static Draft current(Activity activity) {
        Draft draft = DRAFTS.get(activity);
        return draft != null && !activity.isFinishing() && !activity.isDestroyed()
                && draft.owner.get() != null && draft.body.get() != null && draft.body.get().isShown() ? draft : null;
    }

    static void addPostingMenu(Object owner, Menu menu) {
        Activity activity = ExternalBoardAuth.activity(owner);
        if (activity == null || menu == null) return;
        boolean visible = Haiagaru.isPostingOverflowMenu(activity, menu) && current(activity) != null;
        MenuItem existing = menu.findItem(MENU);
        if (existing != null) { existing.setVisible(visible); return; }
        if (!visible) return;
        MenuItem item = menu.add(Menu.NONE, MENU, Menu.NONE, "通常版ChMateで書き込む");
        MENUS.put(item, new WeakReference<>(activity));
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        item.setOnMenuItemClickListener(clicked -> { confirm(activity); return true; });
    }

    static boolean handlePostingMenu(Object owner, MenuItem item) {
        if (item == null || item.getItemId() != MENU) return false;
        Activity activity = ExternalBoardAuth.activity(owner);
        if (activity != null) confirm(activity);
        return true;
    }

    static boolean handlePostingActivityMenu(MenuItem item) {
        if (item == null || item.getItemId() != MENU) return false;
        WeakReference<Activity> ref = MENUS.get(item);
        if (ref != null && ref.get() != null) confirm(ref.get());
        return true;
    }

    static void addSettingsButton(LinearLayout layout, Activity activity) {
        Button button = new Button(activity);
        button.setAllCaps(false); button.setText("通常版ChMateでエッヂに書き込む"); layout.addView(button);
        button.setOnClickListener(view -> message(activity, "通常版ChMateとの書き込み連携",
                "エッヂの書き込み画面で本文・スレタイを入力し、メニューの「通常版ChMateで書き込む」を選びます。\n\n"
                + "通常版の書き込み画面へ渡し、通常版に保存されている認証・レベルで送信できます。root・Shizuku・証明書の追加は不要です。\n\n"
                + "名前・メールは通常版の画面で確認してください。投稿履歴とCookieは通常版で管理されます。Haiagaruの下書きは残るため、投稿後の二重送信に注意してください。"));
    }

    static Intent intent(String target, String body, String subject) {
        StockChmatePostingRules.validate(target, body, subject);
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(target))
                .setClassName(PACKAGE, EDITOR).putExtra(Intent.EXTRA_TEXT, body);
        if (StockChmatePostingRules.newThread(target)) intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        return intent;
    }

    private static void confirm(Activity activity) {
        Draft draft = current(activity);
        if (draft == null) { message(activity, "通常版へ渡せません", "エッヂの書き込み画面から、もう一度お試しください。"); return; }
        try { StockChmatePostingRules.validate(draft.target, draft.body.get().getText().toString(), draft.subject()); }
        catch (IllegalArgumentException invalid) { message(activity, "入力内容を確認", invalid.getMessage()); return; }
        catch (Exception unavailable) { message(activity, "通常版へ渡せません", "スレタイを読み取れませんでした。Haiagaruの下書きは保持しています。"); return; }
        new HaiagaruDialog.Builder(activity).setTitle("通常版ChMateで書き込む")
                .setMessage("投稿先: " + draft.target + "\n\n本文" + (StockChmatePostingRules.newThread(draft.target) ? "とスレタイ" : "")
                        + "を通常版の書き込み画面へ渡します。名前・メールを確認し、通常版の画面で送信してください。\n\n"
                        + "通常版のエッヂ認証・レベルを使います。Haiagaruの下書きは残ります。")
                .setNegativeButton("戻る", null).setPositiveButton("通常版へ渡す", (dialog, which) -> launch(activity, draft)).show();
    }

    private static void launch(Activity activity, Draft draft) {
        if (current(activity) != draft) { message(activity, "通常版へ渡せません", "書き込み画面が変わりました。もう一度お試しください。"); return; }
        final Intent intent;
        try {
            intent = intent(draft.target, draft.body.get().getText().toString(), draft.subject());
            // Persist the original native draft without clearing or claiming that a post succeeded.
            Object owner = draft.owner.get();
            owner.getClass().getMethod("haiagaruSaveExternalDraft").invoke(owner);
        } catch (IllegalArgumentException invalid) { message(activity, "入力内容を確認", invalid.getMessage()); return; }
        catch (Exception unavailable) { message(activity, "通常版へ渡せません", "下書きを保存できませんでした。入力欄の内容を確認してから再試行してください。"); return; }
        try {
            activity.startActivity(intent);
            Toast.makeText(activity, "通常版で内容を確認して送信してください。Haiagaruの下書きは残ります。", Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException | SecurityException unavailable) {
            message(activity, "通常版ChMateを開けません", "同じユーザー領域に通常版ChMateをインストール・有効化してください。Haiagaruの下書きは保持しています。");
        } catch (RuntimeException failed) {
            message(activity, "通常版へ渡せません", "通常版の書き込み画面を開けませんでした。Haiagaruの下書きは保持しています。");
        }
    }

    private static void message(Activity activity, String title, String text) {
        if (!activity.isFinishing() && !activity.isDestroyed()) new HaiagaruDialog.Builder(activity)
                .setTitle(title).setMessage(text).setPositiveButton("閉じる", null).show();
    }
    private StockChmatePosting() {}
}
