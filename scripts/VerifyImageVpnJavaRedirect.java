import app.morphe.extension.chmate.HttpsTransport;
import app.morphe.extension.chmate.ImageVpnController;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/** Real java.net automatic redirect, but two synthetic loopback endpoints only. */
public final class VerifyImageVpnJavaRedirect {
    public static void main(String[] args) throws Exception {
        HttpServer destination = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HttpServer source = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger directRequests = new AtomicInteger();
        ImageVpnController.protectedPort = destination.getAddress().getPort();
        destination.createContext("/image", exchange -> {
            directRequests.incrementAndGet();
            byte[] bytes = new byte[]{1, 2};
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        source.createContext("/start", exchange -> {
            exchange.getResponseHeaders().set("Location", "http://127.0.0.1:" + ImageVpnController.protectedPort + "/image");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        destination.start(); source.start();
        HttpURLConnection connection = null;
        try {
            HttpsTransport.setEnabled(false);
            connection = (HttpURLConnection) HttpsTransport.openConnection(
                    new URL("http://127.0.0.1:" + source.getAddress().getPort() + "/start"));
            connection.setConnectTimeout(2000); connection.setReadTimeout(2000);
            connection.setInstanceFollowRedirects(true);
            try (var input = connection.getInputStream()) { input.readAllBytes(); }
            catch (IOException blocked) { /* Blocking before the synthetic target is acceptable. */ }
            System.out.println("route callbacks=" + ImageVpnController.routes.get()
                    + ", direct synthetic destination requests=" + directRequests.get());
            if (directRequests.get() != 0)
                throw new AssertionError("IMAGE_VPN_JAVA_REDIRECT_BYPASSES_ROUTING: release privacy gate failed");
            System.out.println("PASS: automatic redirect never contacts synthetic protected destination directly");
            HttpServer relay = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            AtomicInteger relayRequests = new AtomicInteger();
            relay.createContext("/proxy.jpg", exchange -> {
                relayRequests.incrementAndGet();
                byte[] bytes = new byte[]{3, 4};
                exchange.sendResponseHeaders(200, bytes.length);
                try (var out = exchange.getResponseBody()) { out.write(bytes); }
            });
            relay.start();
            try {
                ImageVpnController.mappedPort = relay.getAddress().getPort();
                for (boolean forceHttps : new boolean[]{false, true}) {
                    HttpsTransport.setEnabled(false);
                    var mapped = (HttpURLConnection) HttpsTransport.openConnection(
                            new URL("http://127.0.0.1:" + source.getAddress().getPort() + "/start"));
                    mapped.setConnectTimeout(2000); mapped.setReadTimeout(2000);
                    // Force HTTPS on the redirect destination; the mapped relay remains HTTP.
                    HttpsTransport.setEnabled(forceHttps);
                    try (var input = mapped.getInputStream()) {
                        byte[] bytes = input.readAllBytes();
                        if (bytes.length != 2 || bytes[0] != 3) throw new AssertionError("Relay body lost");
                    } finally { mapped.disconnect(); }
                }
                if (directRequests.get() != 0 || relayRequests.get() != 2)
                    throw new AssertionError("Mapped redirect leaked or force-HTTPS broke relay");
                System.out.println("PASS: protected redirects use relay, including force-HTTPS, with zero direct target requests");
            } finally { relay.stop(0); ImageVpnController.mappedPort = 0; HttpsTransport.setEnabled(false); }
        } finally {
            if (connection != null) connection.disconnect();
            source.stop(0); destination.stop(0);
        }
    }
}
