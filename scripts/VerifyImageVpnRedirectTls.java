import app.morphe.extension.chmate.HttpsTransport;
import app.morphe.extension.chmate.ImageVpnController;
import java.net.*;
import java.io.*;
import java.security.cert.Certificate;
import javax.net.ssl.*;
import java.util.*;

/** No sockets: verify HTTPS facade type and caller TLS settings survive a redirect. */
public final class VerifyImageVpnRedirectTls {
    private static final List<FakeTls> connections = new ArrayList<>();
    private static class FakeTls extends HttpsURLConnection {
        FakeTls(URL url) { super(url); connections.add(this); }
        public void connect() { connected = true; }
        public void disconnect() { }
        public boolean usingProxy() { return false; }
        public int getResponseCode() { return url.getPath().equals("/start") ? 302 : 200; }
        public String getHeaderField(String name) {
            return "Location".equalsIgnoreCase(name) && url.getPath().equals("/start") ? "/final" : null;
        }
        public String getCipherSuite() { return "FAKE_NO_REAL_TLS"; }
        public Certificate[] getLocalCertificates() { return null; }
        public Certificate[] getServerCertificates() { return new Certificate[0]; }
        public InputStream getInputStream() { return new ByteArrayInputStream(new byte[]{1}); }
    }
    public static void main(String[] args) throws Exception {
        ImageVpnController.protectedPort = 0;
        HttpsTransport.setEnabled(false);
        URL url = new URL(null, "https://127.0.0.1:8443/start", new URLStreamHandler() {
            protected URLConnection openConnection(URL u) { return new FakeTls(u); }
        });
        URLConnection raw = HttpsTransport.openConnection(url);
        if (!(raw instanceof HttpsURLConnection)) throw new AssertionError("HTTPS cast lost");
        HttpsURLConnection connection = (HttpsURLConnection) raw;
        SSLSocketFactory factory = HttpsURLConnection.getDefaultSSLSocketFactory();
        HostnameVerifier defaultVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
        HostnameVerifier verifier = (host, session) -> defaultVerifier.verify(host, session);
        connection.setSSLSocketFactory(factory); connection.setHostnameVerifier(verifier);
        connection.setRequestProperty("Authorization", "same-origin-synthetic");
        if (connection.getResponseCode() != 200 || connections.size() != 2) throw new AssertionError("TLS redirect");
        for (FakeTls delegate : connections) {
            if (delegate.getSSLSocketFactory() != factory || delegate.getHostnameVerifier() != verifier)
                throw new AssertionError("TLS settings changed");
        }
        if (!"same-origin-synthetic".equals(connections.get(1).getRequestProperty("Authorization")))
            throw new AssertionError("Same-origin authorization lost");
        if (!"FAKE_NO_REAL_TLS".equals(connection.getCipherSuite())) throw new AssertionError("TLS metadata delegation");
        connection.disconnect();
        System.out.println("PASS HTTPS facade, TLS factory/verifier preservation and same-origin headers (no network)");
    }
}
