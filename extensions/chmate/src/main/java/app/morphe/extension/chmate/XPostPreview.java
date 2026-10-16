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
import android.text.style.URLSpan;
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
final class XPostPreview {
    private static final ThreadPoolExecutor WORKER = new ThreadPoolExecutor(2, 2, 30,
            TimeUnit.SECONDS, new ArrayBlockingQueue<>(32), new ThreadPoolExecutor.AbortPolicy());
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LinkedHashMap<String, Entry> CACHE = new LinkedHashMap<>();
    private static volatile int revision;
    private static final class Entry {
        Data data;
        boolean pending;
        long retry;
        final List<WeakReference<TextView>> views = new ArrayList<>();
    }
    private static final class Marker {
        final int revision;
        Marker(int revision) { this.revision = revision; }
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
        if (user == null || text.isEmpty() || text.length() > 20_000) return null;
        String handle = user.optString("screen_name", "");
        if (!handle.matches("[A-Za-z0-9_]{1,50}")) return null;
        return new Data(expectedId, user.optString("name", "").substring(0,
                Math.min(200, user.optString("name", "").length())), handle, text,
                post.optString("created_at", ""));
    }

    static void apply(TextView view) {
        if (view instanceof EditText || Looper.myLooper() != Looper.getMainLooper()) return;
        CharSequence source = view.getText();
        if (!(source instanceof Spanned) || source instanceof Editable) return;
        Spanned spans = (Spanned) source;
        Marker[] markers = spans.getSpans(0, source.length(), Marker.class);
        boolean enabled = Haiagaru.xPostPreviewEnabled();
        if (markers.length > 0) {
            if (enabled && markers[0].revision == revision) return;
            source = source.subSequence(0, spans.getSpanStart(markers[0]));
        }
        if (!enabled) {
            if (markers.length > 0) view.setText(source, TextView.BufferType.SPANNABLE);
            return;
        }
        if (!view.isShown()) return;
        if (source.length() > 100_000) return;
        String[] ids = XMediaAttachments.postIds(source.toString());
        if (ids.length == 0) return;
        SpannableStringBuilder result = null;
        int currentRevision = revision;
        for (String id : ids) {
            Data data = lookup(id, view);
            if (data == null) continue;
            if (result == null) result = new SpannableStringBuilder(source);
            int start = result.length();
            result.append("\n\n");
            int author = result.length();
            result.append(data.author).append("  @").append(data.handle).append("  · X\n");
            result.setSpan(new StyleSpan(Typeface.BOLD), author, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            result.append(data.text);
            if (!data.date.isEmpty()) result.append("\n").append(data.date);
            result.append("\n投稿を開く");
            result.setSpan(new URLSpan("https://x.com/" + data.handle + "/status/" + id),
                    result.length() - 5, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            result.setSpan(new QuoteSpan(view.getCurrentTextColor()), start + 2, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            result.setSpan(new RelativeSizeSpan(.9f), start, result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (result != null) {
            result.setSpan(new Marker(currentRevision), source.length(), result.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            view.setText(result, TextView.BufferType.SPANNABLE);
        } else if (markers.length > 0) view.setText(source, TextView.BufferType.SPANNABLE);
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
            if (entry.data != null) return entry.data;
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
                }
            }
        }
        return null;
    }

    private static void resolve(String id, Entry entry) {
        Data data = null;
        try {
            if (Haiagaru.xPostPreviewEnabled()) {
                String json = XPostMetadata.fetch(id);
                if (json != null) data = parse(json, id);
            }
        } catch (Exception ignored) {
            // Protected/deleted/limited posts keep the original link, not a fake empty tweet.
        }
        List<WeakReference<TextView>> views;
        synchronized (CACHE) {
            entry.data = data;
            entry.pending = false;
            entry.retry = android.os.SystemClock.elapsedRealtime() + 120_000;
            views = new ArrayList<>(entry.views);
            entry.views.clear();
            ++revision;
        }
        MAIN.post(() -> {
            for (WeakReference<TextView> reference : views) {
                TextView view = reference.get();
                if (view != null && view.isAttachedToWindow() && view.isShown()) apply(view);
            }
        });
    }
}
