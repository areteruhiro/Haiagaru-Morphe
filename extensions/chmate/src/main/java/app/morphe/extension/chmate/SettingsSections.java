package app.morphe.extension.chmate;

import android.graphics.Rect;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/** Presentation only: retains the existing controls, listeners and preference keys. */
final class SettingsSections {
    enum Category {
        GENERAL("基本設定", "General", "表示言語・広告", "Language and ads"),
        DISPLAY("スレ・スレ一覧の表示", "Thread display", "行間・フィルタ・並び順・記者ID", "Spacing, filters, ordering, reporter IDs"),
        POSTING("書き込み", "Posting", "文字サイズ・投稿時の動作", "Text size and posting behavior"),
        MEDIA("画像・動画・X／Twitter", "Images, videos and X/Twitter", "メディア取得・ビューア・Imgur", "Media retrieval, viewers and Imgur"),
        CHECKER("必死チェッカー・専用ビューア", "ID checker and dedicated viewer", "検索先・テーマ・文字サイズ・ジェスチャー", "Provider, theme, text size and gestures"),
        NG("NG・省略", "NG and abbreviation", "ワッチョイ・NG管理・ChMate+互換機能", "Wacchoi, NG management and ChMate+"),
        HISTORY("履歴・保存件数", "History and retention", "書き込んだスレ・送信済み・閲覧・検索", "Posted threads, sent posts, browsing and searches"),
        ARCHIVE("過去ログ・エッヂ検索", "Archives and Edge search", "DAT取得・過去ログ検索・開き方", "DAT retrieval, archive search and navigation"),
        BACKUP("バックアップ・データ移行", "Backup and migration", "ローカル・MEGA・パッケージ間の移行", "Local backup, MEGA and package migration"),
        ADVANCED("詳細設定・通信", "Advanced and network", "User-Agent・MonaKey・HTTPS・内部設定", "User-Agent, MonaKey, HTTPS and internals");
        final String ja, en, jaSummary, enSummary;
        Category(String ja, String en, String jaSummary, String enSummary) {
            this.ja = ja; this.en = en; this.jaSummary = jaSummary; this.enSummary = enSummary;
        }
    }
    private static final class Section {
        final List<View> views = new ArrayList<>();
        Button header;
        LinearLayout body;
        String search;
        boolean expanded;
        boolean searchExpanded = true;
    }
    private final LinearLayout layout;
    private final boolean english;
    private final EnumMap<Category, Section> sections = new EnumMap<>(Category.class);
    private Category current;
    private int start;
    private EditText search;
    private String query = "";
    private TextView status;

    SettingsSections(LinearLayout layout, boolean english) { this.layout = layout; this.english = english; }
    void begin(Category category) {
        collect(); current = category; start = layout.getChildCount();
    }
    private void collect() {
        if (current == null) return;
        Section section = sections.get(current);
        if (section == null) { section = new Section(); sections.put(current, section); }
        for (int i = start; i < layout.getChildCount(); i++) section.views.add(layout.getChildAt(i));
    }
    private String text(String ja, String en) { return english ? en : ja; }
    void finish() {
        collect();
        layout.removeAllViews();
        TextView help = new TextView(layout.getContext());
        help.setText(text("カテゴリをタップして開閉できます。変更は「保存」で反映します。別画面を開くボタンには、その画面自身の保存操作があります。",
                "Tap a category to expand it. Apply changes with Save. Separate screens have their own save controls."));
        layout.addView(help);
        search = new EditText(layout.getContext());
        search.setSingleLine(true);
        search.setHint(text("設定を検索", "Search settings"));
        search.setContentDescription(text("Haiagaru設定を検索", "Search Haiagaru settings"));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        layout.addView(search);
        LinearLayout actions = new LinearLayout(layout.getContext());
        Button clear = new Button(layout.getContext()); clear.setText(text("クリア", "Clear"));
        clear.setOnClickListener(v -> search.setText(""));
        Button expand = new Button(layout.getContext()); expand.setText(text("全て開く", "Expand"));
        expand.setOnClickListener(v -> { for (Section s : sections.values()) { s.expanded = true; s.searchExpanded = true; } update(); });
        Button collapse = new Button(layout.getContext()); collapse.setText(text("全て閉じる", "Collapse"));
        collapse.setOnClickListener(v -> { search.setText(""); for (Section s : sections.values()) s.expanded = false; update(); });
        int inset = (int) (8 * layout.getResources().getDisplayMetrics().density + .5f);
        for (Button button : new Button[]{clear, expand, collapse}) {
            button.setAllCaps(false); button.setTextSize(12);
            button.setPadding(inset, button.getPaddingTop(), inset, button.getPaddingBottom());
            actions.addView(button, new LinearLayout.LayoutParams(0, -2, 1));
        }
        layout.addView(actions);
        status = new TextView(layout.getContext()); layout.addView(status);
        for (Category category : Category.values()) {
            Section s = sections.get(category);
            if (s == null || s.views.isEmpty()) continue;
            s.header = new Button(layout.getContext()); s.header.setAllCaps(false);
            s.header.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);
            s.header.setOnClickListener(v -> {
                if (query.trim().isEmpty()) s.expanded = !s.expanded;
                else s.searchExpanded = !s.searchExpanded;
                update();
            });
            layout.addView(s.header);
            s.body = new LinearLayout(layout.getContext()); s.body.setOrientation(LinearLayout.VERTICAL);
            StringBuilder index = new StringBuilder(category.ja + " " + category.en + " " + category.jaSummary + " " + category.enSummary);
            for (View view : s.views) { index.append(' ').append(labels(view)); s.body.addView(view); }
            s.search = index.toString();
            layout.addView(s.body);
        }
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString(); for (Section section : sections.values()) section.searchExpanded = true; update();
            }
            public void afterTextChanged(Editable s) {}
        });
        update();
        // Opening settings should show the category index, not the keyboard.
        layout.setFocusableInTouchMode(true);
        layout.requestFocus();
    }
    // Search labels/help, not editable values, account information or selected spinner values.
    private static String labels(View view) {
        if (view instanceof EditText) return "";
        if (view instanceof TextView) return ((TextView) view).getText().toString();
        if (view instanceof android.widget.Spinner) return "";
        StringBuilder result = new StringBuilder();
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
            result.append(' ').append(labels(((ViewGroup) view).getChildAt(i)));
        return result.toString();
    }
    static boolean matches(String labels, String query) {
        String text = java.text.Normalizer.normalize(labels, java.text.Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        String normalized = java.text.Normalizer.normalize(query, java.text.Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return true;
        for (String word : normalized.split("\\s+")) if (!text.contains(word)) return false;
        return true;
    }
    private void update() {
        int found = 0;
        boolean filtering = !query.trim().isEmpty();
        for (Category c : Category.values()) {
            Section s = sections.get(c); if (s == null || s.header == null) continue;
            boolean visible = matches(s.search, query);
            boolean open = filtering ? s.searchExpanded : s.expanded;
            s.header.setText((open ? "▾ " : "▸ ") + text(c.ja, c.en) + "\n" + text(c.jaSummary, c.enSummary));
            s.header.setContentDescription(text(c.ja, c.en) + text(open ? "、展開中" : "、折りたたみ中", open ? ", expanded" : ", collapsed"));
            s.header.setVisibility(visible ? View.VISIBLE : View.GONE);
            s.body.setVisibility(visible && open ? View.VISIBLE : View.GONE);
            if (visible) found++;
        }
        status.setText(found == 0 ? text("該当する設定がありません。別の言葉で検索してください。", "No matching settings. Try another word.")
                : text(filtering ? "一致したカテゴリを表示しています（" : "カテゴリ（", filtering ? "Matching categories (" : "Categories (") + found + ")");
    }
    void reveal(View target) {
        search.setText("");
        for (Section s : sections.values()) {
            for (View parent = target; parent != null; parent = parent.getParent() instanceof View ? (View) parent.getParent() : null)
                if (parent == s.body) s.expanded = true;
        }
        update(); target.requestFocus();
        target.post(() -> target.requestRectangleOnScreen(new Rect(0, 0, target.getWidth(), target.getHeight()), false));
    }
    void revealCategory(Category category) {
        search.setText("");
        Section s = sections.get(category);
        if (s != null) s.expanded = true;
        update();
        if (s != null && s.header != null) s.header.post(() -> s.header.requestRectangleOnScreen(
                new Rect(0, 0, s.header.getWidth(), s.header.getHeight()), false));
    }
}
