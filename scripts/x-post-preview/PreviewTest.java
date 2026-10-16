package app.morphe.extension.chmate;

import android.content.Context;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.util.Map;

/** Runs the real preview code with Android TextView/Spanned; no real downloads. */
public final class PreviewTest {
    static final class ShownTextView extends TextView {
        ShownTextView(Context context) { super(context); }
        @Override public boolean isShown() { return getVisibility() == android.view.View.VISIBLE; }
    }
    static int checks;
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        if (Looper.getMainLooper() == null) Looper.prepareMainLooper();
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        Object thread = activityThread.getMethod("systemMain").invoke(null);
        Context context = (Context) activityThread.getMethod("getSystemContext").invoke(thread);
        String json = "{\"id_str\":\"123\",\"text\":\"本文 + / <script>\\n2行目\","
                + "\"user\":{\"name\":\"投稿者\",\"screen_name\":\"example\"},\"created_at\":\"2026-10-16\"}";
        XPostPreview.Data data = XPostPreview.parse(json, "123");
        check(data != null && data.text.equals("本文 + / <script>\n2行目"), "Literal text, symbols and newline");
        check(XPostPreview.parse(json, "999") == null, "Mismatched tweet ID");
        check(XPostPreview.parse("{}", "123") == null, "Unavailable tweet");
        check(XPostPreview.parse(json.replace("example", "evil/path"), "123") == null, "Invalid author path");
        check(XPostPreview.parse(json.replace("本文 + / <script>\\n2行目", ""), "123") == null, "Empty tweet");

        Class<?> entryType = Class.forName("app.morphe.extension.chmate.XPostPreview$Entry");
        var constructor = entryType.getDeclaredConstructor(); constructor.setAccessible(true);
        Object entry = constructor.newInstance();
        Field dataField = entryType.getDeclaredField("data"); dataField.setAccessible(true); dataField.set(entry, data);
        Field cache = XPostPreview.class.getDeclaredField("CACHE"); cache.setAccessible(true);
        ((Map) cache.get(null)).put("123", entry);
        String original = "元のレス\nhttps://x.com/example/status/123/photo/1\n末尾";
        SpannableStringBuilder source = new SpannableStringBuilder(original);
        int start = original.indexOf("https:"), end = original.indexOf("\n末尾");
        URLSpan originalLink = new URLSpan(original.substring(start, end));
        source.setSpan(originalLink, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView view = new ShownTextView(context);
        view.setText(source, TextView.BufferType.SPANNABLE);
        Haiagaru.enabled = false;
        XPostPreview.apply(view);
        check(view.getText().toString().equals(original), "OFF retains original text");
        Haiagaru.enabled = true;
        XPostPreview.apply(view);
        check(view.getText().toString().startsWith(original + "\n\n"), "Inline preview appended only");
        check(view.getText().toString().contains("投稿者  @example"), "Author shown");
        check(view.getText().toString().contains(data.text), "Tweet literal content shown");
        check(source.toString().equals(original), "Native source not mutated");
        Spanned displayed = (Spanned) view.getText();
        check(displayed.getSpanStart(originalLink) == start && displayed.getSpanEnd(originalLink) == end,
                "Original URL span offsets preserved");
        String first = view.getText().toString();
        XPostPreview.apply(view);
        check(view.getText().toString().equals(first), "Repeated layout does not duplicate preview");
        Haiagaru.enabled = false;
        XPostPreview.apply(view);
        check(view.getText().toString().equals(original), "Turning OFF removes preview");
        Haiagaru.enabled = true;
        XPostPreview.apply(view);
        check(view.getText().toString().equals(first), "Turning ON restores one preview");
        view.setText(new SpannableStringBuilder("別のレス https://example.com/"), TextView.BufferType.SPANNABLE);
        XPostPreview.apply(view);
        check(!view.getText().toString().contains("投稿者"), "Recycled row has no old tweet");
        view.setText(source, TextView.BufferType.SPANNABLE);
        view.setVisibility(android.view.View.GONE);
        XPostPreview.apply(view);
        check(view.getText().toString().equals(original), "Hidden body receives no preview");
        System.out.println("PASS: " + checks + " Android tweet-preview checks; no network or account access.");
        System.exit(0);
    }
}
