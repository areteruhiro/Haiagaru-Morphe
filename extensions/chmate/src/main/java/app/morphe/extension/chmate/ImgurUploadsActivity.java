package app.morphe.extension.chmate;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.*;

/** Native account media browser. Does not embed a login WebView. */
public final class ImgurUploadsActivity extends Activity {
    private ScrollView uploadsView;
    private LinearLayout uploadsRows;
    private TextView status;
    private int uploadsGeneration;
    private int currentPage;
    private boolean deletingUploads;
    private volatile boolean stopUploadDeletion;
    private ImgurThumbnailLoader thumbnails;
    private final java.util.ArrayList<android.view.View> deletionControls = new java.util.ArrayList<>();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (!ImgurWebUpload.supported(this)) { finish(); return; }
        ImgurWebUpload.init(this);
        currentPage = state == null ? 0 : Math.max(0, Math.min(10000, state.getInt("page", 0)));
        thumbnails = new ImgurThumbnailLoader();
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(12);
        root.setPadding(padding, padding, padding, padding);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(padding, padding + insets.getSystemWindowInsetTop(), padding,
                    padding + insets.getSystemWindowInsetBottom()); return insets;
        });
        TextView title = new TextView(this); title.setText("Imgur投稿一覧"); title.setTextSize(20); root.addView(title);
        LinearLayout toolbar = new LinearLayout(this); root.addView(toolbar);
        button(toolbar, "戻る", this::finish);
        button(toolbar, "ログイン・設定", () -> startActivity(new android.content.Intent(this, ImgurWebLoginActivity.class)));
        button(toolbar, "更新", () -> showUploads(currentPage));
        status = new TextView(this); root.addView(status);
        uploadsView = new ScrollView(this);
        uploadsRows = new LinearLayout(this); uploadsRows.setOrientation(LinearLayout.VERTICAL);
        uploadsView.addView(uploadsRows);
        root.addView(uploadsView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }
    @Override protected void onResume() {
        super.onResume();
        if (uploadsRows != null && !deletingUploads) showUploads(currentPage);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putInt("page", currentPage);
    }
    @Override protected void onStop() {
        if (thumbnails != null) thumbnails.reset();
        super.onStop();
    }
    @Override public void onBackPressed() { if (!deletionBusy()) super.onBackPressed(); }
    @Override protected void onDestroy() {
        stopUploadDeletion = true; uploadsGeneration++;
        if (thumbnails != null) thumbnails.close();
        super.onDestroy();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private Button button(LinearLayout parent, String title, Runnable action) {
        Button button = new Button(this); button.setText(title); button.setOnClickListener(view -> action.run());
        parent.addView(button, parent.getOrientation() == LinearLayout.HORIZONTAL
                ? new LinearLayout.LayoutParams(0, -2, 1) : new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private void showUploads(int page) {
        if (deletionBusy()) return;
        currentPage = page;
        thumbnails.reset();
        final int generation = ++uploadsGeneration;
        uploadsView.setVisibility(android.view.View.VISIBLE);
        uploadsRows.removeAllViews();
        TextView heading = new TextView(this);
        heading.setText("Imgurアップロード一覧（非公開画像・動画を含む）\n読み込み中…");
        uploadsRows.addView(heading);
        new Thread(() -> {
            try {
                ImgurWebUpload.UploadPage snapshot = ImgurWebUpload.uploadPage(page);
                org.json.JSONArray rows = snapshot.rows;
                java.util.Map<String, java.util.List<String>> threads = ImgurUploadHistory.threads(this, snapshot.accountId);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || uploadsGeneration != generation) return;
                    heading.setText("Imgurアップロード一覧（非公開画像・動画を含む）\n" + (page + 1) + "ページ目");
                    LinearLayout pager = new LinearLayout(this); uploadsRows.addView(pager);
                    button(pager, "前へ", () -> showUploads(page - 1)).setEnabled(page > 0);
                    button(pager, "更新", () -> showUploads(page));
                    button(pager, "次へ", () -> showUploads(page + 1)).setEnabled(rows.length() > 0 && page < 10000);
                    java.util.LinkedHashSet<String> selected = new java.util.LinkedHashSet<>();
                    java.util.ArrayList<CheckBox> choices = new java.util.ArrayList<>();
                    CheckBox all = new CheckBox(this); all.setText("このページを全選択／全解除");
                    uploadsRows.addView(all);
                    TextView selectionNote = new TextView(this);
                    selectionNote.setText("チェックした投稿をImgurサーバーから削除します。ページ移動・更新で選択は解除されます。");
                    uploadsRows.addView(selectionNote);
                    Button delete = button(uploadsRows, "選択した投稿を削除（0件）", () -> confirmUploadDeletion(snapshot,
                            selected.toArray(new String[0]), page, generation));
                    delete.setEnabled(false);
                    boolean[] changingAll = {false};
                    all.setOnCheckedChangeListener((view, checked) -> {
                        if (changingAll[0]) return;
                        changingAll[0] = true;
                        for (CheckBox choice : choices) choice.setChecked(checked);
                        changingAll[0] = false;
                    });
                    if (rows.length() == 0) {
                        TextView empty = new TextView(this); empty.setText("このページに投稿はありません。"); uploadsRows.addView(empty);
                    }
                    for (int i = 0; i < rows.length(); i++) {
                        org.json.JSONObject row = rows.optJSONObject(i);
                        if (row == null) continue;
                        String url = row.optString("link", "");
                        boolean pending = ImgurWebPolicy.processing(row.opt("processing"));
                        String id = row.optString("id", "");
                        CheckBox title = new CheckBox(this);
                        String label = row.optString("title", "");
                        if (label.isEmpty() || "null".equals(label)) label = row.optString("id", "画像／動画");
                        String date = row.optLong("datetime", 0) > 0
                                ? android.text.format.DateFormat.getDateFormat(this).format(new java.util.Date(row.optLong("datetime") * 1000L)) + " " : "";
                        title.setText(date + label + (pending ? "\n処理中（更新して確認してください）" : "\n" + url));
                        LinearLayout mediaRow = new LinearLayout(this);
                        mediaRow.setPadding(0, dp(12), 0, 0); uploadsRows.addView(mediaRow);
                        LinearLayout preview = new LinearLayout(this); preview.setOrientation(LinearLayout.VERTICAL);
                        mediaRow.addView(preview, new LinearLayout.LayoutParams(dp(112), -2));
                        ImageView thumbnail = new ImageView(this); thumbnail.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        preview.addView(thumbnail, new LinearLayout.LayoutParams(-1, dp(96)));
                        TextView kind = new TextView(this);
                        boolean video = row.optString("type", "").startsWith("video/") || url.endsWith(".mp4") || url.endsWith(".webm");
                        kind.setText(pending ? "処理中" : video ? "動画" : "画像"); preview.addView(kind);
                        mediaRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
                        thumbnails.load(ImgurWebPolicy.thumbnailUrl(id, url, pending), thumbnail);
                        thumbnail.setOnClickListener(view -> { if (title.isEnabled()) title.toggle(); });
                        title.setEnabled(ImgurWebPolicy.imageId(id));
                        if (title.isEnabled()) {
                            choices.add(title);
                            title.setOnCheckedChangeListener((view, checked) -> {
                                if (checked) selected.add(id); else selected.remove(id);
                                delete.setText("選択した投稿を削除（" + selected.size() + "件）");
                                delete.setEnabled(!selected.isEmpty());
                                if (!changingAll[0]) {
                                    changingAll[0] = true;
                                    all.setChecked(!choices.isEmpty() && selected.size() == choices.size());
                                    changingAll[0] = false;
                                }
                            });
                        }
                        Button copy = new Button(this); copy.setText("URLをコピー");
                        copy.setEnabled(!pending && ImgurWebPolicy.mediaUrl(url));
                        copy.setOnClickListener(view -> {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            if (clipboard != null) {
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Imgur URL", url));
                                Toast.makeText(this, "URLをコピーしました", Toast.LENGTH_SHORT).show();
                            }
                        });
                        uploadsRows.addView(copy);
                        java.util.List<String> related = threads.get(url);
                        if (ImgurUploadHistory.threadsEnabled(this) && related != null && !related.isEmpty()) {
                            TextView note = new TextView(this);
                            note.setText("アップロード元スレ（掲示板への投稿完了は未確認）"); uploadsRows.addView(note);
                            for (String thread : related) button(uploadsRows, thread, () -> {
                                try {
                                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(thread));
                                    intent.setClass(this, OpenUrlActivity.class); startActivity(intent);
                                } catch (RuntimeException error) {
                                    Toast.makeText(this, "スレを開けませんでした", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                    all.setEnabled(!choices.isEmpty());
                    uploadsView.scrollTo(0, 0);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed() || uploadsGeneration != generation) return;
                    heading.setText("投稿一覧を取得できません。ログイン状態と通信を確認してください。\n" + error.getMessage());
                    button(uploadsRows, "再読み込み", () -> showUploads(page));
                });
            }
        }, "Haiagaru-Imgur-uploads").start();
    }

    private boolean deletionBusy() {
        if (!deletingUploads) return false;
        Toast.makeText(this, "削除中です。結果が出るまでお待ちください", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void blockDeletionControls(android.view.View view) {
        if (view instanceof Button && view.isEnabled()) {
            deletionControls.add(view); view.setEnabled(false);
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) blockDeletionControls(group.getChildAt(i));
        }
    }

    private void confirmUploadDeletion(ImgurWebUpload.UploadPage snapshot, String[] ids, int page, int generation) {
        if (deletionBusy() || ids.length == 0 || generation != uploadsGeneration) return;
        new AlertDialog.Builder(this).setTitle("Imgurサーバーから" + ids.length + "件を削除しますか？")
                .setMessage("画像・動画そのものを削除します。取り消せません。掲示板等に貼ったURLからも見られなくなります。"
                        + "\n端末内の元ファイルは削除しません。対象はこのページでチェックした投稿だけです。")
                .setNegativeButton("キャンセル", null)
                .setPositiveButton("サーバーから削除", (dialog, which) -> {
                    if (deletingUploads || generation != uploadsGeneration || isFinishing() || isDestroyed()) return;
                    deletingUploads = true; stopUploadDeletion = false;
                    blockDeletionControls(findViewById(android.R.id.content));
                    status.setText("Imgurサーバーから削除しています…（自動再試行はしません）");
                    new Thread(() -> {
                        String message;
                        try {
                            ImgurWebUpload.DeleteResult result = ImgurWebUpload.deleteUploads(snapshot, ids, () -> stopUploadDeletion);
                            message = "削除完了：" + result.deleted + "件\n結果未確認：" + result.unconfirmed
                                    + "件\n未実行：" + result.notAttempted + "件";
                            if (result.unconfirmed > 0) message += "\n通信切断などで削除済みの場合があります。再実行する前に一覧を確認してください。";
                        } catch (Exception error) {
                            message = "削除を開始できませんでした。ログイン状態を確認し、一覧を更新してください。";
                        }
                        final String summary = message;
                        runOnUiThread(() -> {
                            deletingUploads = false;
                            if (isFinishing() || isDestroyed()) return;
                            for (android.view.View control : deletionControls) control.setEnabled(true);
                            deletionControls.clear();
                            status.setText(summary);
                            showUploads(page); // Read-only refresh; never repeats a DELETE.
                            new AlertDialog.Builder(this).setTitle("削除結果").setMessage(summary).setPositiveButton("閉じる", null).show();
                        });
                    }, "Haiagaru-Imgur-delete").start();
                }).show();
    }
}
