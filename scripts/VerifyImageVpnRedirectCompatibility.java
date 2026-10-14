import app.morphe.extension.chmate.HttpsTransport;
import app.morphe.extension.chmate.ImageVpnController;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

/** Real HTTP behaviour on loopback only, with the production guarded connection. */
public final class VerifyImageVpnRedirectCompatibility {
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
    private static HttpURLConnection open(String url) throws Exception {
        var connection = (HttpURLConnection) HttpsTransport.openConnection(new URL(url));
        connection.setConnectTimeout(2000); connection.setReadTimeout(2000);
        return connection;
    }
    public static void main(String[] args) throws Exception {
        ImageVpnController.protectedPort = 0;
        HttpsTransport.setEnabled(false);
        var source = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var target = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        String base = "http://127.0.0.1:" + source.getAddress().getPort();
        String result = "http://127.0.0.1:" + target.getAddress().getPort() + "/capture";
        AtomicReference<String> observed = new AtomicReference<>();
        AtomicInteger requests = new AtomicInteger();
        target.createContext("/capture", exchange -> {
            requests.incrementAndGet();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            observed.set(exchange.getRequestMethod() + ":" + body + ":"
                + exchange.getRequestHeaders().getFirst("Authorization") + ":"
                + exchange.getRequestHeaders().getFirst("Cookie") + ":"
                + exchange.getRequestHeaders().getFirst("X-Check"));
            exchange.getResponseHeaders().set("X-Result", "final");
            byte[] bytes = "OK".getBytes(StandardCharsets.US_ASCII);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var out = exchange.getResponseBody()) { out.write(bytes); }
        });
        for (int code : new int[]{302, 303, 307, 308}) {
            source.createContext("/r" + code, exchange -> {
                exchange.getRequestBody().readAllBytes();
                exchange.getResponseHeaders().set("Location", result);
                exchange.sendResponseHeaders(code, -1); exchange.close();
            });
        }
        source.createContext("/loop", exchange -> {
            exchange.getResponseHeaders().set("Location", "/loop");
            exchange.sendResponseHeaders(302, -1); exchange.close();
        });
        source.start(); target.start();
        try {
            var get = open(base + "/r302");
            get.setRequestProperty("Authorization", "synthetic-secret");
            get.setRequestProperty("Cookie", "synthetic-cookie");
            get.setRequestProperty("X-Check", "kept");
            check(get.getResponseCode() == 200, "GET redirect");
            check("GET::null:null:kept".equals(observed.get()), "Cross-origin credentials or ordinary header");
            check("final".equals(get.getHeaderField("X-Result")), "Final headers");
            check(get.getURL().toString().equals(result), "Final URL");
            check(new String(get.getInputStream().readAllBytes(), StandardCharsets.US_ASCII).equals("OK"), "Final body");
            get.disconnect();
            for (int code : new int[]{302, 303, 307, 308}) {
                var post = open(base + "/r" + code);
                post.setRequestMethod("POST"); post.setDoOutput(true);
                try (var out = post.getOutputStream()) { out.write("body-data".getBytes(StandardCharsets.UTF_8)); }
                check(post.getResponseCode() == 200, "POST redirect " + code);
                check(observed.get().startsWith(code <= 303 ? "GET::" : "POST:body-data:"), "Method/body " + code);
                post.disconnect();
            }
            int before = requests.get();
            var noFollow = open(base + "/r302"); noFollow.setInstanceFollowRedirects(false);
            check(noFollow.getResponseCode() == 302 && requests.get() == before, "Disabled redirects");
            noFollow.disconnect();
            var stream = open(base + "/r307"); stream.setRequestMethod("POST"); stream.setDoOutput(true);
            stream.setFixedLengthStreamingMode(2);
            try (var out = stream.getOutputStream()) { out.write(new byte[]{1, 2}); }
            try { stream.getResponseCode(); throw new AssertionError("Streaming body replayed"); }
            catch (HttpRetryException expected) { check(requests.get() == before, "Stream replay destination reached"); }
            stream.disconnect();
            var loop = open(base + "/loop");
            try { loop.getResponseCode(); throw new AssertionError("Unbounded redirects"); }
            catch (ProtocolException expected) { }
            loop.disconnect();
            System.out.println("PASS normal GET, POST 302/303/307/308, credential stripping, final headers, no-follow, streaming retry and redirect bounds");
        } finally { source.stop(0); target.stop(0); }
    }
}
