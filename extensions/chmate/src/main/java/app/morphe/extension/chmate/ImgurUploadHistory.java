package app.morphe.extension.chmate;

import android.content.Context;
import java.util.*;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/** Native history remains owned/written by ChMate. Only associations are stored here. */
public final class ImgurUploadHistory {
    private static volatile Context context;
    private static final LinkedHashMap<String, Long> pending = new LinkedHashMap<>();
    private static final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
    static void init(Context value) { context = value.getApplicationContext(); }
    static boolean nativeEnabled(Context c) { return ImgurWebUpload.prefs(c).getBoolean("nativeUploadHistory", true); }
    static boolean threadsEnabled(Context c) { return ImgurWebUpload.prefs(c).getBoolean("uploadThreadHistory", false); }

    static synchronized void uploaded(String link, long account) {
        pending.put(link, account);
        while (pending.size() > 256) pending.remove(pending.keySet().iterator().next());
    }

    /** Replaces exactly the native successful-upload row append, never URL insertion. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static boolean addNativeHistory(Collection history, Object row) {
        Context c = context;
        try {
            String link = (String) row.getClass().getField("link").get(row);
            Long account;
            synchronized (ImgurUploadHistory.class) { account = pending.remove(link); }
            if (account != null && c != null) {
                if (threadsEnabled(c)) {
                    String target = (String) row.getClass().getField("target").get(row);
                    String thread = threadUrl(target);
                    if (thread != null) worker.execute(() -> saveThread(c, account, link, thread));
                }
                if (!nativeEnabled(c)) return false;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Keep the native success callback usable if a future model changes.
        }
        return history.add(row);
    }

    // A target can also be a board or an empty new-thread draft. Do not label those as threads.
    static String threadUrl(String value) {
        if (value == null || value.length() > 2048) return null;
        try {
            java.net.URI uri = new java.net.URI(value);
            if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null) return null;
            String path = uri.getPath();
            if (path == null || !(path.matches("/test/read\\.cgi/[^/]+/[0-9]{9,}(/.*)?")
                    || path.matches("/bbs/read\\.cgi/[^/]+/[0-9]+/[0-9]{9,}(/.*)?")
                    || path.matches("/boards/[^/]+/[0-9]{9,}(/.*)?"))) return null;
            // Do not retain query parameters or fragments in a local history entry.
            return new java.net.URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), path, null, null).toString();
        } catch (Exception ignored) { return null; }
    }

    private static void saveThread(Context c, long account, String link, String thread) {
        if (!threadsEnabled(c)) return;
        try {
            android.content.SharedPreferences prefs = c.getSharedPreferences("haiagaru_imgur_threads", 0);
            JSONArray old = new JSONArray(prefs.getString("items", "[]"));
            JSONArray next = new JSONArray();
            next.put(new JSONObject().put("account", account).put("link", link).put("thread", thread));
            for (int i = 0; i < old.length() && next.length() < 1000; i++) {
                JSONObject item = old.optJSONObject(i);
                if (item == null || (item.optLong("account") == account && link.equals(item.optString("link"))
                        && thread.equals(item.optString("thread")))) continue;
                next.put(item);
            }
            prefs.edit().putString("items", next.toString()).apply();
        } catch (Exception ignored) { /* Association failure must never fail an upload. */ }
    }

    /** Call on the list worker, scoped to the account whose server media were fetched. */
    static Map<String, List<String>> threads(Context c, long account) {
        Map<String, List<String>> result = new HashMap<>();
        if (!threadsEnabled(c)) return result;
        try {
            JSONArray rows = new JSONArray(c.getSharedPreferences("haiagaru_imgur_threads", 0).getString("items", "[]"));
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null || row.optLong("account") != account) continue;
                String thread = threadUrl(row.optString("thread"));
                if (thread == null) continue;
                String link = row.optString("link");
                List<String> urls = result.get(link);
                if (urls == null) { urls = new ArrayList<>(); result.put(link, urls); }
                if (!urls.contains(thread)) urls.add(thread);
            }
        } catch (Exception ignored) {}
        return result;
    }
}
