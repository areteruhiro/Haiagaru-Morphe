package app.morphe.extension.chmate;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.*;

/** Portable archive identities. Never substitutes a guessed 5ch server for a missing one. */
public final class ArchiveOriginPaths {
    private static final Pattern THREAD = Pattern.compile("/(?:test/read\\.(?:cgi|php)|bbs/read(?:_archive)?\\.cgi|boards)/.+?/(\\d{9,13})(?:/.*)?$");
    private ArchiveOriginPaths() { }
    public static String threadKey(String url) {
        try {
            if (url == null || url.length() > 8192) return null;
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null) return null;
            Matcher m = THREAD.matcher(uri.getPath()); return m.find() ? m.group(1) : null;
        } catch (Exception ignored) { return null; }
    }
    public static String relative(File root, File file) throws IOException {
        String base = root.getCanonicalPath() + File.separator;
        String path = file.getCanonicalPath();
        if (!path.startsWith(base) || !file.getAbsolutePath().equals(path) || !path.endsWith(".dat"))
            throw new IOException("アーカイブ保存領域外です");
        return path.substring(base.length()).replace(File.separatorChar, '/');
    }
    public static String hash(String relative) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(relative.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder(); for (byte b : digest) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return out.toString() + ".json";
    }
    public static String reconstruct(String relative) {
        try {
            String[] parts = relative.split("/"); if (parts.length != 3) return null;
            String board = URLDecoder.decode(parts[0], "UTF-8");
            int slash = board.indexOf('/'); if (slash <= 0) return null;
            String host = board.substring(0, slash), name = board.substring(slash + 1);
            if (!host.matches("[a-zA-Z0-9.-]+(?::[0-9]{1,5})?") || !name.matches("[a-zA-Z0-9_-]+(?:/[0-9]+)?")) return null;
            Matcher key = Pattern.compile("\\[(\\d{9,13})\\]$").matcher(parts[1]);
            if (!key.find() || !parts[2].matches(Pattern.quote(parts[0] + "_" + key.group(1)) + "(?:[1-9][0-9]?)?\\.dat")) return null;
            String route = host.equals("talk.jp") ? "/boards/" : name.contains("/") ? "/bbs/read.cgi/" : "/test/read.cgi/";
            String url = "https://" + host + route + name + "/" + key.group(1) + "/";
            return threadKey(url) == null ? null : url;
        } catch (Exception ignored) { return null; }
    }
}
