package app.morphe.extension.chmate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/** Per-image routing decision. Not wired into native downloads until a verified tunnel exists. */
public final class ImageVpnRequestPolicy {
    public static final String TARGET_HOST = "tools.yajuvideo.st";
    private ImageVpnRequestPolicy() {}

    public interface Tunnel {
        /** The backend must resolve DNS and connect inside the tunnel, never via default sockets. */
        URLConnection open(URL url) throws IOException;
    }

    public static boolean isTarget(URL url) {
        return url != null && TARGET_HOST.equalsIgnoreCase(url.getHost());
    }

    public static boolean isProtectedUrl(String value) {
        try {
            URL url = new URL(value);
            validateWebUrl(url);
            return isTarget(url);
        } catch (Exception invalid) { return false; }
    }

    /** A ticket cannot change to direct routing after redirects, disconnects or setting changes. */
    public static Ticket begin(URL imageUrl, boolean enabled) throws IOException {
        validateWebUrl(imageUrl);
        return new Ticket(enabled && isTarget(imageUrl));
    }

    public static final class Ticket {
        private boolean tunnelRequired;
        private int redirects;
        private Ticket(boolean tunnelRequired) { this.tunnelRequired = tunnelRequired; }
        public boolean requiresTunnel() { return tunnelRequired; }

        /** Called before opening any redirect destination; a protected download stays protected. */
        public URL redirect(URL current, String location, boolean protectionEnabled) throws IOException {
            if (++redirects > 5) throw new IOException("IMAGE_VPN_REDIRECT_LIMIT");
            if (location == null || location.trim().isEmpty()) throw new IOException("IMAGE_VPN_INVALID_REDIRECT");
            validateWebUrl(current);
            URL next = new URL(current, location);
            validateWebUrl(next);
            if (protectionEnabled && isTarget(next)) tunnelRequired = true;
            return next;
        }

        /** Protected calls have no direct-connect implementation or automatic fallback. */
        public URLConnection openProtected(URL url, Tunnel tunnel) throws IOException {
            validateWebUrl(url);
            if (!tunnelRequired) throw new IOException("IMAGE_VPN_NOT_A_PROTECTED_REQUEST");
            if (tunnel == null) throw new IOException("IMAGE_VPN_TUNNEL_UNAVAILABLE");
            URLConnection connection = tunnel.open(url);
            if (connection == null) throw new IOException("IMAGE_VPN_TUNNEL_UNAVAILABLE");
            if (connection instanceof java.net.HttpURLConnection) {
                // Redirects must go through redirect() and this same ticket, not a native fallback.
                ((java.net.HttpURLConnection) connection).setInstanceFollowRedirects(false);
            }
            return connection;
        }
    }

    private static void validateWebUrl(URL url) throws IOException {
        if (url == null) throw new IOException("IMAGE_VPN_INVALID_URL");
        try {
            URI uri = url.toURI();
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443)) {
                throw new IOException("IMAGE_VPN_INVALID_URL");
            }
        } catch (URISyntaxException error) {
            throw new IOException("IMAGE_VPN_INVALID_URL", error);
        }
    }
}
