package app.morphe.extension.chmate;

import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Native, display-only previews. Never changes DAT, NG input or outgoing post text. */
public final class XPostPreview {
    static void addCacheSettings(android.widget.LinearLayout layout, android.app.Activity activity) {
        android.widget.TextView note = new android.widget.TextView(activity);
        note.setText("X投稿プレビューの取得済みデータは端末内に7日間保存します（最大128投稿・16MiB）。再起動後やオフライン時も有効なキャッシュを使用します。投稿が変更・削除されても保存期間中は以前の内容を表示する場合があります。画像・動画ファイルのキャッシュとは別です。");
        layout.addView(note);
        android.widget.Button button = new android.widget.Button(activity);
        button.setText("X投稿プレビューのキャッシュを削除"); layout.addView(button);
        button.setOnClickListener(v -> new android.app.AlertDialog.Builder(activity)
                .setMessage("保存済みのX投稿プレビューを削除しますか？次に表示する際は再取得します。")
                .setNegativeButton("キャンセル", null).setPositiveButton("削除", (d, w) -> {
                    button.setEnabled(false);
                    new Thread(() -> {
                        XPostMetadata.clear();
                        MAIN.post(() -> {
                            synchronized (CACHE) { CACHE.clear(); ++revision; }
                            if (!activity.isFinishing() && !activity.isDestroyed()) {
                                button.setEnabled(true);
                                android.widget.Toast.makeText(activity,"X投稿プレビューのキャッシュを削除しました",android.widget.Toast.LENGTH_SHORT).show();
                            }
                        });
                    }, "Haiagaru-X-cache-clear").start();
                }).show());
    }
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(2, 2, 30,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), new ThreadPoolExecutor.AbortPolicy());
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LinkedHashMap<String, Entry> CACHE = new LinkedHashMap<>();
    private static volatile int revision;
    private static final java.util.WeakHashMap<TextView, Boolean> BOUND = new java.util.WeakHashMap<>();
    private static final java.util.WeakHashMap<TextView, RowState> ROWS = new java.util.WeakHashMap<>();
    // Native body TextViews call onBound from their overridden setText. Our own
    // preview writes must not enter that binding hook again (191/226/242).
    private static final ThreadLocal<TextView> WRITING = new ThreadLocal<>();
    private static final class RowState {
        final String source;
        final java.util.HashSet<String> expanded = new java.util.HashSet<>();
        RowState(String source) { this.source = source; }
    }
    private static final class Entry {
        Data data;
        long expires;
        boolean pending;
        long retry;
        int failures;
        final List<WeakReference<TextView>> views = new ArrayList<>();
    }
    private static final class Marker {
        final int revision;
        final boolean full;
        final boolean collapsible;
        Marker(int revision) {
            this.revision = revision;
            this.full = Haiagaru.xPostPreviewFullText();
            this.collapsible = Haiagaru.xPostPreviewCollapsible();
        }
    }
    static final class Data {
        final String id, author, handle, text, date;
        Data(String id, String author, String handle, String text, String date) {
            this.id = id; this.author = author; this.handle = handle; this.text = text; this.date = date;
        }
    }

    static Data parse(String json, String expectedId) throws Exception {
        JSONObject post = new JSONObject(json);
        if (!expectedId.equals(post.optString("id_str"))) return null;
        JSONObject user = post.optJSONObject("user");
        String text = post.optString("text", "");
        String full = post.optString("full_text", "");
        JSONObject extended = post.optJSONObject("extended_tweet");
        if (extended != null && extended.optString("full_text").length() > full.length())
            full = extended.optString("full_text");
        JSONObject note = post.optJSONObject("note_tweet");
        if (note != null) {
            JSONObject results = note.optJSONObject("note_tweet_results");
            JSONObject result = results == null ? null : results.optJSONObject("result");
            String noteText = result == null ? note.optString("text", "") : result.optString("text", "");
            if (noteText.length() > full.length()) full = noteText;
        }
        if (full.length() > text.length()) text = full;
        if (user == null || text.isEmpty() || text.length() > 50_000) return null;
        String handle = user.optString("screen_name", "");
        if (!handle.matches("[A-Za-z0-9_]{1,50}")) return null;
        return new Data(expectedId, user.optString("name", "").substring(0,
                Math.min(200, user.optString("name", "").length())), handle, text,
                post.optString("created_at", ""));
    }

    static void apply(TextView view) {
        apply(view, false);
    }

    private static void apply(TextView view, boolean force) {
        if (view instanceof EditText || Looper.myLooper() != Looper.getMainLooper()) return;
        CharSequence source = view.getText();
        if (!(source instanceof Spanned) || source instanceof Editable) return;
        Spanned spans = (Spanned) source;
        Marker[] markers = spans.getSpans(0, source.length(), Marker.class);
        boolean enabled = Haiagaru.xPostPreviewEnabled();
        if (markers.length > 0) {
            if (!force && enabled && markers[0].revision == revision && markers[0].full == Haiagaru.xPostPreviewFullText()
                    && markers[0].collapsible == Haiagaru.xPostPreviewCollapsible()) return;
            source = source.subSequence(0, spans.getSpanStart(markers[0]));
        }
        if (!enabled) {
            if (markers.length > 0) setText(view, source);
            return;
        }
        if (!view.isShown()) return;
        if (source.length() > 100_000) return;
        String[] ids = XMediaAttachments.postIds(source.toString());
        if (ids.length == 0) return;
        CharSequence result = decorate(source, view, true);
        if (result != source) setText(view, result);
        else if (markers.length > 0) setText(view, source);
    }

    /** Warm previews must be present before native row measurement/restored pixel offsets. */
    static CharSequence prepare(CharSequence source) {
        return prepare(source, null);
    }

    private static CharSequence prepare(CharSequence source, TextView view) {
        if (!Haiagaru.xPostPreviewEnabled() || !(source instanceof Spanned)
                || source.length() > 100_000) return source;
        Spanned spanned = (Spanned) source;
        Marker[] markers = spanned.getSpans(0, source.length(), Marker.class);
        if (markers.length > 0) source = source.subSequence(0, spanned.getSpanStart(markers[0]));
        return decorate(source, view, false);
    }

    /** Called after native binding; covers recycled rows even when no global layout fires. */
    public static void onBound(Object object) {
        if (object == WRITING.get()) return;
        DirectBodySelection.prepare(object);
        if (!(object instanceof TextView) || object instanceof EditText || !Haiagaru.xPostPreviewEnabled()) return;
        TextView view = (TextView) object;
        CharSequence source = view.getText();
        CharSequence cached = prepare(source, view);
        if (cached != source) setText(view, cached);
        if (BOUND.put(view, Boolean.TRUE) != null) return;
        WeakReference<TextView> reference = new WeakReference<>(view);
        view.post(() -> {
            TextView current = reference.get();
            if (current == null) return;
            BOUND.remove(current);
            apply(current);
        });
    }

    private static void setText(TextView view, CharSequence source) {
        TextView previous = WRITING.get();
        WRITING.set(view);
        try {
            XPostPreviewPosition.beforeChange(view);
            view.setText(source, TextView.BufferType.SPANNABLE);
        } finally {
            if (previous == null) WRITING.remove();
            else WRITING.set(previous);
        }
    }

    static void toggle(TextView view, String id) {
        CharSequence source=view.getText();
        if (!(source instanceof Spanned)) return;
        Spanned spans=(Spanned)source;
        Marker[] markers=spans.getSpans(0,source.length(),Marker.class);
        if(markers.length>0)source=source.subSequence(0,spans.getSpanStart(markers[0]));
        if (!java.util.Arrays.asList(XMediaAttachments.postIds(source.toString())).contains(id)) return;
        RowState state=rowState(view,source);
        if(!state.expanded.remove(id))state.expanded.add(id);
        apply(view,true);
    }

    private static RowState rowState(TextView view, CharSequence source) {
        RowState state=ROWS.get(view);
        String key=source.toString();
        if(state==null || !state.source.equals(key)) { state=new RowState(key); ROWS.put(view,state); }
        return state;
    }

    private static CharSequence decorate(CharSequence source, TextView view, boolean fetch) {
        SpannableStringBuilder result = null;
        int currentRevision = revision;
        boolean collapsible=Haiagaru.xPostPreviewCollapsible();
        for (String id : XMediaAttachments.postIds(source.toString())) {
            boolean expanded=!collapsible || view!=null && rowState(view,source).expanded.contains(id);
            Data data;
            if (fetch && expanded && view != null) data = lookup(id, view);
            else synchronized (CACHE) { Entry entry = CACHE.get(id); data = entry == null || android.os.SystemClock.elapsedRealtime() >= entry.expires ? null : entry.data; }
            if (data == null && !collapsible) continue;
            if (result == null) result = new SpannableStringBuilder(source);
            int start = result.length();
            result.append("\n\n");
            if(collapsible) {
                int toggleStart=result.length();
                result.append(expanded ? "▼ X投稿のプレビューを閉じる" : "▶ X投稿のプレビューを表示");
                result.setSpan(new XPostPreviewTouch.ToggleLink(id),toggleStart,result.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                if(!expanded)continue;
                result.append("\n");
                if(data==null) { result.append("読み込み中／取得できない場合は元のURLを開いてください"); continue; }
            }
            int author = result.length();
            result.append(data.author).append("  @").append(data.handle).append("  · X\n");
            result.setSpan(new StyleSpan(Typeface.BOLD), author, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            String body = data.text;
            if (!Haiagaru.xPostPreviewFullText() && body.codePointCount(0, body.length()) > 140)
                body = body.substring(0, body.offsetByCodePoints(0, Math.min(140, body.codePointCount(0, body.length())))) + "…";
            result.append(body);
            if (!data.date.isEmpty()) result.append("\n").append(data.date);
            result.append("\n投稿を開く");
            result.setSpan(new XPostPreviewTouch.PostLink("https://x.com/" + data.handle + "/status/" + id),
                    result.length() - 5, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            result.setSpan(new QuoteSpan(), start + 2, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            result.setSpan(new RelativeSizeSpan(.9f), start, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (result != null) {
            result.setSpan(new Marker(currentRevision), source.length(), result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return result;
        }
        return source;
    }

    private static Data lookup(String id, TextView view) {
        Entry entry;
        synchronized (CACHE) {
            entry = CACHE.get(id);
            if (entry == null) {
                if (CACHE.size() >= 128) {
                    String remove = null;
                    for (java.util.Map.Entry<String, Entry> old : CACHE.entrySet())
                        if (!old.getValue().pending) { remove = old.getKey(); break; }
                    if (remove == null) return null;
                    CACHE.remove(remove);
                }
                entry = new Entry();
                CACHE.put(id, entry);
            }
            if (entry.data != null && android.os.SystemClock.elapsedRealtime() < entry.expires) return entry.data;
            entry.data = null;
            if (!entry.pending && android.os.SystemClock.elapsedRealtime() < entry.retry) return null;
            entry.views.removeIf(reference -> reference.get() == null);
            boolean subscribed = false;
            for (WeakReference<TextView> reference : entry.views) if (reference.get() == view) subscribed = true;
            if (!subscribed && entry.views.size() < 32) entry.views.add(new WeakReference<>(view));
            if (!entry.pending) {
                entry.pending = true;
                final Entry target = entry;
                try { WORKER.execute(() -> resolve(id, target)); }
                catch (java.util.concurrent.RejectedExecutionException full) {
                    entry.pending = false;
                    entry.retry = android.os.SystemClock.elapsedRealtime() + 10_000;
                    scheduleRetry(entry, 10_000);
                }
            }
        }
        return null;
    }

    private static void resolve(String id, Entry entry) {
        Data data = null;
        try {
            if (Haiagaru.xPostPreviewEnabled()) {
                String json = XPostMetadata.fetchPreview(id);
                if (json != null) data = parse(json, id);
            }
        } catch (Exception ignored) {
            // Protected/deleted/limited posts keep the original link, not a fake empty tweet.
        }
        List<WeakReference<TextView>> views;
        synchronized (CACHE) {
            entry.data = data;
            entry.expires = android.os.SystemClock.elapsedRealtime() + 900_000;
            entry.pending = false;
            entry.failures = data == null ? entry.failures + 1 : 0;
            entry.retry = android.os.SystemClock.elapsedRealtime() + (entry.failures <= 2 ? 10_000 : 120_000);
            views = new ArrayList<>(entry.views);
            entry.views.clear();
            if (data == null && entry.failures <= 2) entry.views.addAll(views);
            ++revision;
        }
        MAIN.post(() -> {
            for (WeakReference<TextView> reference : views) {
                TextView view = reference.get();
                if (view != null && view.isAttachedToWindow() && view.isShown()) apply(view);
            }
        });
        if (data == null && entry.failures <= 2) scheduleRetry(entry, 10_100);
    }

    private static void scheduleRetry(Entry entry, long delay) {
        MAIN.postDelayed(() -> {
            List<WeakReference<TextView>> views;
            synchronized (CACHE) { views = new ArrayList<>(entry.views); entry.views.clear(); ++revision; }
            if (!Haiagaru.xPostPreviewEnabled()) return;
            for (WeakReference<TextView> reference : views) {
                TextView view = reference.get();
                if (view != null && view.isAttachedToWindow() && view.isShown()) apply(view);
            }
        }, delay);
    }
}
