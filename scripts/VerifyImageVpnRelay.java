import app.morphe.extension.chmate.ImageVpnFetchManager;
import app.morphe.extension.chmate.ImageVpnRelay;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Loopback only with fake backend; never contacts an image site or VPN relay. */
public final class VerifyImageVpnRelay {
    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(1000); connection.setReadTimeout(3000);
        connection.setInstanceFollowRedirects(false);
        return connection;
    }
    private static void status(String url, int status) throws IOException {
        var connection = open(url);
        try {
            if (connection.getResponseCode() != status || connection.getHeaderField("Location") != null)
                throw new AssertionError("Wrong status or unsafe redirect");
        } finally { connection.disconnect(); }
    }
    public static void main(String[] args) throws Exception {
        ImageVpnFetchManager backend = new ImageVpnFetchManager();
        ImageVpnRelay relay = new ImageVpnRelay(backend);
        String target = "https://tools.yajuvideo.st/offline-only.png";
        String local = relay.register(target);
        if (!local.equals(relay.register(target)) || local.contains("yajuvideo")) throw new AssertionError("Capability mapping");
        status(local, 200);
        status("http://127.0.0.1:" + relay.port() + "/image/not-a-capability.jpg?url=" + target, 410);
        if (backend.calls != 1) throw new AssertionError("Arbitrary forwarding");
        try { relay.register("https://example.com/image.png"); throw new AssertionError("Non-target registered"); }
        catch (IOException expected) { }
        try (Socket client = new Socket("127.0.0.1", relay.port())) {
            client.setSoTimeout(3000);
            client.getOutputStream().write(("GET " + new URL(local).getPath()
                + " HTTP/1.1\r\nHost: evil.test\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            String result = new String(client.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
            if (!result.startsWith("HTTP/1.1 403")) throw new AssertionError("Host/DNS rebinding protection");
        }
        backend.fail = true;
        status(local, 503);
        backend.fail = false;
        for (int index = 0; index < 128; index++) relay.register(target + "?fixture=" + index);
        status(local, 410);
        relay.close();
        if (!backend.closed) throw new AssertionError("Tunnel left active");
        try { relay.register(target); throw new AssertionError("Disabled registration"); }
        catch (IOException expected) { }
        System.out.println("PASS offline image relay: opaque capabilities, bounded entries, no arbitrary forwarding, no error redirects and cleanup");
    }
}
