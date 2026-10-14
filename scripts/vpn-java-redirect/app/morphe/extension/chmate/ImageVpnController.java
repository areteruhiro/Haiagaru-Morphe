package app.morphe.extension.chmate;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

/** Test double: all sockets are loopback, and the synthetic destination must be blocked. */
public final class ImageVpnController {
    public static int protectedPort;
    public static int mappedPort;
    public static final AtomicInteger routes = new AtomicInteger();
    public static boolean enabled() { return true; }
    public static boolean isLocalEndpoint(String host, int port) { return "127.0.0.1".equals(host) && port == mappedPort && mappedPort > 0; }
    public static URL route(URL url) throws IOException {
        routes.incrementAndGet();
        if (!"127.0.0.1".equals(url.getHost())) throw new IOException("TEST_EXTERNAL_NETWORK_FORBIDDEN");
        if (url.getPort() == protectedPort) {
            if (mappedPort > 0) return new URL("http://127.0.0.1:" + mappedPort + "/proxy.jpg");
            throw new IOException("TEST_PROTECTED_DESTINATION_BLOCKED");
        }
        return url;
    }
}
