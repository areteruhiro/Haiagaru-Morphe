package app.morphe.extension.chmate;

import java.net.URI;

/** Scope the TCP race to standard board/thread GETs. */
public final class UpdateConnectPolicy {
    private UpdateConnectPolicy() { }

    public static boolean isThreadResource(String method, String url) {
        if (!"GET".equals(method) || url == null) return false;
        try {
            URI uri = URI.create(url);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) return false;
            String path = uri.getPath();
            return path != null && (path.endsWith("/subject.txt") || path.endsWith("/subject.json")
                    || path.matches(".*/(?:dat|kako)/[0-9/]+\\.dat")
                    || path.matches("/test/(?:read|r)\\.cgi/[^/]+/[0-9]+(?:/.*)?"));
        } catch (IllegalArgumentException error) { return false; }
    }
}
