package app.morphe.extension.chmate;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.util.Map;
import android.view.MotionEvent;
import android.view.View;

/** Runs the real preview code with Android TextView/Spanned; no real downloads. */
public final class PreviewTest {
    static final class ShownTextView extends TextView {
        ShownTextView(Context context) {
            super(context);
            setLayoutParams(new android.view.ViewGroup.LayoutParams(900, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        @Override public boolean isShown() { return getVisibility() == android.view.View.VISIBLE; }
    }
    static int checks;
    /** Matches native ChMate body setText -> injected onBound, unlike plain TextView. */
    static final class BoundTextView extends TextView {
        boolean armed, failWrite;
        int depth, maximumDepth, bindings;
        BoundTextView(Context context) { super(context); armed = true; }
        @Override public boolean isShown() { return true; }
        @Override public void setText(CharSequence text, BufferType type) {
            if (failWrite) { failWrite = false; throw new IllegalStateException("test write failed"); }
            super.setText(text, type);
            if (!armed) return;
            bindings++;
            depth++;
            maximumDepth = Math.max(maximumDepth, depth);
            try {
                if (depth > 8) throw new AssertionError("Recursive native preview binding");
                XPostPreview.onBound(this);
            } finally { depth--; }
        }
    }
    static final class RecordingContext extends ContextWrapper {
        Intent opened;
        RecordingContext(Context context) { super(context); }
        @Override public void startActivity(Intent intent) { opened = intent; }
    }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        if (Looper.getMainLooper() == null) Looper.prepareMainLooper();
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        Object thread = activityThread.getMethod("systemMain").invoke(null);
        Context context = (Context) activityThread.getMethod("getSystemContext").invoke(thread);
        run(context);
        System.exit(0);
    }
    public static void run(Context context) throws Exception {
        try { runChecks(context); }
        catch (Throwable error) {
            android.util.Log.e("HaiagaruPreviewTest", "FAIL", error);
            throw error;
        }
    }
    static void runChecks(Context context) throws Exception {
        String json = "{\"id_str\":\"123\",\"text\":\"本文 + / <script>\\n2行目\","
                + "\"user\":{\"name\":\"投稿者\",\"screen_name\":\"example\"},\"created_at\":\"2026-10-16\"}";
        XPostPreview.Data data = XPostPreview.parse(json, "123");
        check(data != null && data.text.equals("本文 + / <script>\n2行目"), "Literal text, symbols and newline");
        check(XPostPreview.parse(json, "999") == null, "Mismatched tweet ID");
        check(XPostPreview.parse("{}", "123") == null, "Unavailable tweet");
        check(XPostPreview.parse(json.replace("example", "evil/path"), "123") == null, "Invalid author path");
        check(XPostPreview.parse(json.replace("本文 + / <script>\\n2行目", ""), "123") == null, "Empty tweet");
        String longText = "全文テスト😀".repeat(100);
        String extended = new org.json.JSONObject(json).put("full_text", longText).toString();
        check(XPostPreview.parse(extended, "123").text.equals(longText), "Full text takes precedence over excerpt");
        String note = new org.json.JSONObject(json).put("note_tweet", new org.json.JSONObject()
                .put("note_tweet_results", new org.json.JSONObject().put("result", new org.json.JSONObject().put("text",longText)))).toString();
        check(XPostPreview.parse(note, "123").text.equals(longText), "Long-form note text retained");

        Class<?> entryType = Class.forName("app.morphe.extension.chmate.XPostPreview$Entry");
        var constructor = entryType.getDeclaredConstructor(); constructor.setAccessible(true);
        Object entry = constructor.newInstance();
        Field dataField = entryType.getDeclaredField("data"); dataField.setAccessible(true); dataField.set(entry, data);
        Field expiresField = entryType.getDeclaredField("expires"); expiresField.setAccessible(true); expiresField.setLong(entry, android.os.SystemClock.elapsedRealtime()+900000);
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
        CharSequence prepared=XPostPreview.prepare(source);
        check(prepared.toString().equals(first), "Warm preview rendered before native measurement");
        check(XPostPreview.prepare(prepared).toString().equals(first), "Warm rebind never duplicates preview");
        dataField.set(entry, XPostPreview.parse(extended,"123"));
        Haiagaru.full=false;
        view.setText(source,TextView.BufferType.SPANNABLE);XPostPreview.apply(view);
        check(!view.getText().toString().contains(longText) && view.getText().toString().contains("…"), "Excerpt mode is bounded");
        Haiagaru.full=true;XPostPreview.apply(view);
        check(view.getText().toString().contains(longText), "Full text toggle refreshes existing row");
        RecordingContext recording=new RecordingContext(context);
        TextView clickable=new ShownTextView(recording);
        dataField.set(entry,data);
        clickable.setText(XPostPreview.prepare(source),TextView.BufferType.SPANNABLE);
        clickable.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(20000,View.MeasureSpec.AT_MOST));
        clickable.layout(0,0,900,clickable.getMeasuredHeight());
        Spanned links=(Spanned)clickable.getText();
        XPostPreviewTouch.PostLink link=links.getSpans(0,links.length(),XPostPreviewTouch.PostLink.class)[0];
        int offset=links.getSpanStart(link)+1,line=clickable.getLayout().getLineForOffset(offset);
        float x=clickable.getLayout().getPrimaryHorizontal(offset)+clickable.getTotalPaddingLeft();
        float y=(clickable.getLayout().getLineTop(line)+clickable.getLayout().getLineBottom(line))/2f+clickable.getTotalPaddingTop();
        MotionEvent down=MotionEvent.obtain(100,100,MotionEvent.ACTION_DOWN,x,y,0);
        MotionEvent up=MotionEvent.obtain(100,150,MotionEvent.ACTION_UP,x,y,0);
        check(XPostPreviewTouch.onTouch(clickable,down), "Own post link consumes down");
        check(XPostPreviewTouch.onTouch(clickable,up), "Own post link consumes up");
        check(recording.opened!=null && "https://x.com/example/status/123".equals(recording.opened.getDataString()), "Post link opens exact original post");
        check((recording.opened.getFlags()&Intent.FLAG_ACTIVITY_NEW_TASK)!=0,"Post opened in separate task");
        recording.opened=null;
        XPostPreviewTouch.onTouch(clickable,down);
        MotionEvent moved=MotionEvent.obtain(100,125,MotionEvent.ACTION_MOVE,x,y+200,0);
        XPostPreviewTouch.onTouch(clickable,moved);XPostPreviewTouch.onTouch(clickable,up);
        check(recording.opened==null,"Dragging link never opens post");
        MotionEvent nativeDown=MotionEvent.obtain(200,200,MotionEvent.ACTION_DOWN,2,2,0);
        check(!XPostPreviewTouch.onTouch(clickable,nativeDown),"Native text touches not intercepted");
        down.recycle();up.recycle();moved.recycle();nativeDown.recycle();
        Haiagaru.collapsible=true;
        clickable.setText(source,TextView.BufferType.SPANNABLE);
        XPostPreview.apply(clickable);
        check(clickable.getText().toString().contains("▶ X投稿") && !clickable.getText().toString().contains(data.text), "Collapsed preview hides cached body");
        tapToggle(clickable);
        check(clickable.getText().toString().contains("▼ X投稿") && clickable.getText().toString().contains(data.text), "Tap expands preview");
        check(recording.opened==null,"Expanding never opens browser");
        tapToggle(clickable);
        check(!clickable.getText().toString().contains(data.text),"Second tap collapses preview");
        tapToggle(clickable);
        clickable.setText(new SpannableStringBuilder("別のレス "+original),TextView.BufferType.SPANNABLE);
        XPostPreview.apply(clickable);
        check(!clickable.getText().toString().contains(data.text),"Recycling resets expansion");
        int calls=XPostMetadata.calls;
        clickable.setText(new SpannableStringBuilder("https://x.com/example/status/999"),TextView.BufferType.SPANNABLE);
        XPostPreview.apply(clickable);
        check(clickable.getText().toString().contains("▶ X投稿") && XPostMetadata.calls==calls,"Uncached collapsed preview does not fetch metadata");
        clickable.setText(source,TextView.BufferType.SPANNABLE);
        XPostPreview.apply(clickable);
        Haiagaru.collapsible=false;
        XPostPreview.apply(clickable);
        check(clickable.getText().toString().contains(data.text) && !clickable.getText().toString().contains("▶ X投稿"),"Setting OFF restores expanded display");
        view.setText(new SpannableStringBuilder("別のレス https://example.com/"), TextView.BufferType.SPANNABLE);
        XPostPreview.apply(view);
        check(!view.getText().toString().contains("投稿者"), "Recycled row has no old tweet");
        view.setText(source, TextView.BufferType.SPANNABLE);
        view.setVisibility(android.view.View.GONE);
        XPostPreview.apply(view);
        check(view.getText().toString().equals(original), "Hidden body receives no preview");
        checkNativeReentry(context, source, original, data.text);
        System.out.println("PASS: " + checks + " Android tweet-preview checks; no network or account access.");
    }

    static void checkNativeReentry(Context context, CharSequence source, String original, String body) {
        Haiagaru.enabled = true; Haiagaru.collapsible = false;
        BoundTextView nativeView = new BoundTextView(context);
        nativeView.setText(source, TextView.BufferType.SPANNABLE);
        check(nativeView.maximumDepth == 2 && nativeView.bindings == 2, "Native setText reentry is bounded");
        check(nativeView.getText().toString().contains(body), "Cached preview survives native binding");
        String expected = nativeView.getText().toString();
        for (int i = 0; i < 20; i++) XPostPreview.onBound(nativeView);
        check(nativeView.getText().toString().equals(expected), "Repeated native binding never duplicates preview");
        check(nativeView.maximumDepth == 2, "Repeated rebind does not grow the stack");
        Haiagaru.collapsible = true;
        nativeView.setText(source, TextView.BufferType.SPANNABLE);
        check(nativeView.getText().toString().contains("▶ X投稿"), "Collapsed native binding remains usable");
        XPostPreview.toggle(nativeView, "123");
        check(nativeView.getText().toString().contains(body), "Native expand does not recurse");
        XPostPreview.toggle(nativeView, "123");
        check(!nativeView.getText().toString().contains(body), "Native collapse does not recurse");
        nativeView.setText(new SpannableStringBuilder("https://x.com/example/status/999"), TextView.BufferType.SPANNABLE);
        check(nativeView.getText().toString().contains("▶ X投稿"), "Uncached collapsed native binding does not recurse");
        Haiagaru.collapsible = false;
        nativeView.armed = false;
        nativeView.setText(source, TextView.BufferType.SPANNABLE);
        nativeView.armed = true; nativeView.failWrite = true;
        try { XPostPreview.onBound(nativeView); throw new AssertionError("Expected test write failure"); }
        catch (IllegalStateException expectedFailure) { checks++; }
        XPostPreview.onBound(nativeView);
        check(nativeView.getText().toString().contains(body), "Write guard is cleared after an exception");
        nativeView.setText(new SpannableStringBuilder("別のレス"), TextView.BufferType.SPANNABLE);
        check(nativeView.getText().toString().equals("別のレス"), "Recycled native row removes previous preview");
        nativeView.setText(source, TextView.BufferType.SPANNABLE);
        Haiagaru.enabled = false; XPostPreview.apply(nativeView);
        check(nativeView.getText().toString().equals(original), "OFF removes preview through native binding");
        Haiagaru.enabled = true;
    }

    static void tapToggle(TextView view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(20000,View.MeasureSpec.AT_MOST));
        view.layout(0,0,900,view.getMeasuredHeight());
        Spanned text=(Spanned)view.getText();
        XPostPreviewTouch.ToggleLink toggle=text.getSpans(0,text.length(),XPostPreviewTouch.ToggleLink.class)[0];
        int offset=text.getSpanStart(toggle)+1,line=view.getLayout().getLineForOffset(offset);
        float x=view.getLayout().getPrimaryHorizontal(offset)+view.getTotalPaddingLeft();
        float y=(view.getLayout().getLineTop(line)+view.getLayout().getLineBottom(line))/2f+view.getTotalPaddingTop();
        MotionEvent down=MotionEvent.obtain(300,300,MotionEvent.ACTION_DOWN,x,y,0);
        MotionEvent up=MotionEvent.obtain(300,350,MotionEvent.ACTION_UP,x,y,0);
        check(XPostPreviewTouch.onTouch(view,down) && XPostPreviewTouch.onTouch(view,up),"Toggle handles tap");
        down.recycle();up.recycle();
    }
}
