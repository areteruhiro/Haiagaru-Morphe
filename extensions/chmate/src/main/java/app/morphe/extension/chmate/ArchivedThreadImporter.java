package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Imports an archived HTML thread into ChMate's own DAT cache. */
final class ArchivedThreadImporter {
    private static final String LOG_TAG = "HaiagaruArchive";
    private static final Charset MS932 = Charset.forName("MS932");
    private static final int MAX_RESPONSE_BYTES = 16 * 1024 * 1024;
    private static final Pattern THREAD_URL = Pattern.compile(
            "^https?://([a-z0-9_-]+)\\.([a-z0-9.-]+)/test/read\\.(?:cgi|php)/"
                    + "([a-zA-Z0-9_-]+)/(\\d{9,10})(?:/.*)?$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TITLE = Pattern.compile(
            "<h1[^>]*id=[\"']threadtitle[\"'][^>]*>(.*?)</h1>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern POST = Pattern.compile(
            "<div[^>]*id=[\"'](\\d+)[\"'][^>]*class=[\"'][^\"']*\\bpost\\b[^\"']*[\"'][^>]*>"
                    + ".*?<span[^>]*class=[\"']postusername[\"'][^>]*><b>"
                    + "(.*?)</b>.*?<span[^>]*class=[\"']date[\"'][^>]*>(.*?)</span>"
                    + "(?:<span[^>]*class=[\"']uid[\"'][^>]*>(.*?)</span>)?.*?"
                    + "</details>\\s*<section[^>]*class=[\"']post-content[\"'][^>]*>"
                    + "(.*?)</section>\\s*</div>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern TAG = Pattern.compile("<[^>]+>", Pattern.DOTALL);
    private static final Pattern BREAK = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern IMAGE = Pattern.compile(
            "(?i)<img[^>]+src=[\"'](?:https?:)?//([^\"']+)[\"'][^>]*>"
    );
    private static final Pattern SCRIPT = Pattern.compile(
            "(?is)<(?:script|style)[^>]*>.*?</(?:script|style)>"
    );
    private static final Set<String> IN_FLIGHT =
            Collections.synchronizedSet(new HashSet<>());
    private static final SecureRandom RANDOM = new SecureRandom();

    private ArchivedThreadImporter() {
    }

    static boolean importIfNeeded(Activity activity, String originalUrl, String browserFallback) {
        ThreadInfo info = ThreadInfo.parse(originalUrl);
        if (info == null) return false;

        File directory = activity.getExternalFilesDir("2chMate/dat");
        if (directory == null) return false;
        File datFile = new File(directory, info.board + "_" + info.thread + ".dat");
        if (datFile.isFile() && datFile.length() > 0) {
            Log.i(LOG_TAG, "Using existing cached DAT " + datFile.getName()
                    + " (" + datFile.length() + " bytes)");
            return true;
        }

        String importKey = info.board + ":" + info.thread;
        if (!IN_FLIGHT.add(importKey)) return true;

        Toast.makeText(activity, "過去ログを取得しています…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                byte[] dat = fetchArchivedDat(info);
                if (!directory.isDirectory() && !directory.mkdirs()) {
                    throw new IOException("Unable to create ChMate DAT directory");
                }
                File temporary = new File(directory, datFile.getName() + ".haiagaru.tmp");
                try (FileOutputStream output = new FileOutputStream(temporary, false)) {
                    output.write(dat);
                    output.getFD().sync();
                }
                if (datFile.exists() && !datFile.delete()) {
                    throw new IOException("Unable to replace existing ChMate DAT");
                }
                if (!temporary.renameTo(datFile)) {
                    throw new IOException("Unable to publish imported ChMate DAT");
                }
                File index = new File(directory, info.board + "_" + info.thread + ".idx");
                if (index.exists() && !index.delete()) {
                    Log.w(LOG_TAG, "Unable to remove stale index " + index.getName());
                }
                Log.i(LOG_TAG, "Imported " + dat.length + " DAT bytes for " + importKey);
                reopen(activity, originalUrl, "過去ログを取得しました");
            } catch (Throwable error) {
                Log.e(LOG_TAG, "Unable to import archived thread " + importKey, error);
                reopen(activity, browserFallback, "過去ログを自動取得できませんでした");
            } finally {
                IN_FLIGHT.remove(importKey);
            }
        }, "Haiagaru-archive-import").start();
        return true;
    }

    private static void reopen(Activity activity, String url, String message) {
        activity.runOnUiThread(() -> {
            if (activity.isFinishing()) return;
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            Intent retry = new Intent(activity.getIntent());
            retry.setData(Uri.parse(url));
            retry.putExtra("haiagaru.archive.retry", true);
            activity.startActivity(retry);
            activity.finish();
        });
    }

    private static byte[] fetchArchivedDat(ThreadInfo info) throws Exception {
        Throwable kakoFailure;
        try {
            return convertKakoHtml(info, request(
                    "https://kako.5ch.io/test/read.cgi/" + info.board + "/"
                            + info.thread + "/",
                    MS932
            ));
        } catch (Throwable error) {
            kakoFailure = error;
            Log.w(LOG_TAG, "kako route failed", error);
        }

        try {
            String endpoint = "https://itest.5ch.io/public/newapi/client.php?subdomain="
                    + encode(info.server) + "&board=" + encode(info.board)
                    + "&dat=" + encode(info.thread) + "&rand=" + randomToken();
            return convertItestJson(request(endpoint, StandardCharsets.UTF_8));
        } catch (Throwable error) {
            Log.w(LOG_TAG, "itest route failed", error);
        }

        try {
            String scUrl = "https://" + info.server + ".2ch.sc/" + info.board
                    + "/dat/" + info.thread + ".dat";
            byte[] raw = requestBytes(scUrl);
            if (raw.length < 16) throw new IOException("2ch.sc returned an empty DAT");
            return raw;
        } catch (Throwable error) {
            error.addSuppressed(kakoFailure);
            throw error;
        }
    }

    private static byte[] convertKakoHtml(ThreadInfo info, String html) throws IOException {
        Matcher titleMatcher = TITLE.matcher(html);
        if (!titleMatcher.find()) throw new IOException("Archive title was not found");
        String title = plainText(titleMatcher.group(1));

        StringBuilder dat = new StringBuilder(Math.max(16 * 1024, html.length() / 2));
        Matcher posts = POST.matcher(html);
        int count = 0;
        while (posts.find()) {
            String name = plainText(posts.group(2));
            String date = plainText(posts.group(3));
            String uid = plainText(posts.group(4));
            String message = datMessage(posts.group(5));
            dat.append(sanitizeField(name)).append("<>")
                    .append("<>")
                    .append(sanitizeField(date));
            if (!uid.isEmpty()) dat.append(' ').append(sanitizeField(uid));
            dat.append("<>").append(message).append("<>");
            if (count == 0) dat.append(sanitizeField(title));
            dat.append('\n');
            count++;
        }
        if (count == 0) throw new IOException("Archive posts were not found");
        Log.i(LOG_TAG, "Converted " + count + " posts from kako for "
                + info.board + ":" + info.thread);
        return dat.toString().getBytes(MS932);
    }

    private static byte[] convertItestJson(String body) throws Exception {
        if (body == null || body.trim().isEmpty()) {
            throw new IOException("itest returned an empty response");
        }
        JSONObject root = new JSONObject(body);
        JSONArray thread = root.getJSONArray("thread");
        JSONArray comments = root.getJSONArray("comments");
        String title = thread.optString(5, "");
        StringBuilder dat = new StringBuilder(comments.length() * 128);
        for (int index = 0; index < comments.length(); index++) {
            JSONArray comment = comments.getJSONArray(index);
            dat.append(sanitizeField(comment.optString(1, ""))).append("<>")
                    .append(sanitizeField(comment.optString(2, ""))).append("<>")
                    .append(sanitizeField(comment.optString(3, "")));
            String uid = comment.optString(4, "");
            if (!uid.isEmpty()) dat.append(uid.contains("ID:") ? " " : " ID:").append(uid);
            String be = comment.optString(5, "");
            if (!be.isEmpty()) dat.append(" BE:").append(be);
            dat.append("<>").append(comment.optString(6, "")).append("<>");
            if (index == 0) dat.append(sanitizeField(title));
            dat.append('\n');
        }
        if (comments.length() == 0) throw new IOException("itest returned no posts");
        return dat.toString().getBytes(MS932);
    }

    private static String request(String url, Charset charset) throws IOException {
        return new String(requestBytes(url), charset);
    }

    private static byte[] requestBytes(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(6_000);
        connection.setReadTimeout(15_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 Haiagaru");
        try {
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code + " from " + url);
            }
            try (BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[16 * 1024];
                int total = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_RESPONSE_BYTES) {
                        throw new IOException("Archive response was too large");
                    }
                    output.write(buffer, 0, count);
                }
                return output.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String datMessage(String html) {
        String value = SCRIPT.matcher(html).replaceAll("");
        value = IMAGE.matcher(value).replaceAll("sssp://$1");
        value = BREAK.matcher(value).replaceAll("\n");
        value = plainText(value);
        return value.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\n", "<br>")
                .trim();
    }

    @SuppressWarnings("deprecation")
    private static String plainText(String html) {
        if (html == null || html.isEmpty()) return "";
        return Html.fromHtml(TAG.matcher(html).replaceAll("")).toString().trim();
    }

    private static String sanitizeField(String value) {
        return value == null ? "" : value.replace("<>", "＜＞")
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
    }

    private static String encode(String value) throws IOException {
        return URLEncoder.encode(value, "UTF-8");
    }

    private static String randomToken() {
        final char[] alphabet =
                "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz0123456789".toCharArray();
        StringBuilder token = new StringBuilder(10);
        for (int index = 0; index < 10; index++) {
            token.append(alphabet[RANDOM.nextInt(alphabet.length)]);
        }
        return token.toString();
    }

    private static final class ThreadInfo {
        final String server;
        final String board;
        final String thread;

        ThreadInfo(String server, String board, String thread) {
            this.server = server;
            this.board = board;
            this.thread = thread;
        }

        static ThreadInfo parse(String url) {
            Matcher matcher = THREAD_URL.matcher(url == null ? "" : url);
            if (!matcher.matches()) return null;
            return new ThreadInfo(matcher.group(1), matcher.group(3), matcher.group(4));
        }
    }
}
