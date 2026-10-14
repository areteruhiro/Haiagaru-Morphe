import app.morphe.extension.chmate.VpnGateCatalog;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/** Offline catalogue fixtures: no HTTP connections, fake endpoints only. */
public final class VerifyVpnGateCatalog {
    private static String row(String host, String ip, String profile) {
        String encoded = java.util.Base64.getEncoder().encodeToString(profile.getBytes(StandardCharsets.US_ASCII));
        return host + "," + ip + ",1000,1,1,Test,XX,1,1,1,1,x,x,x," + encoded;
    }
    private static void rejected(String csv) {
        if (!VpnGateCatalog.parse(csv).isEmpty()) throw new AssertionError("Accepted invalid fixture");
    }
    public static void main(String[] args) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((java.security.KeyStore)null);
        String ca = null;
        for (TrustManager manager : factory.getTrustManagers()) {
            if (!(manager instanceof X509TrustManager)) continue;
            for (X509Certificate cert : ((X509TrustManager)manager).getAcceptedIssuers()) {
                try { cert.checkValidity(); } catch (Exception expired) { continue; }
                if (cert.getBasicConstraints() < 0) continue;
                ca = "-----BEGIN CERTIFICATE-----\n" + java.util.Base64.getEncoder().encodeToString(cert.getEncoded())
                        + "\n-----END CERTIFICATE-----";
                break;
            }
        }
        if (ca == null) throw new AssertionError("Fixture requires a valid JVM trust root");
        String profile = "proto tcp-client\nremote 8.8.8.8 1195\n<ca>\n" + ca + "\n</ca>\n";
        String csv = row("fixture-vpn", "8.8.8.8", profile);
        var servers = VpnGateCatalog.parse(csv);
        if (servers.size() != 1 || servers.get(0).port != 443
                || !servers.get(0).hostname.equals("fixture-vpn.opengw.net")) throw new AssertionError("SSTP endpoint");
        if (VpnGateCatalog.parse(csv + "\n" + csv).size() != 1) throw new AssertionError("Duplicate candidate");
        rejected(row("bad.host", "8.8.8.8", profile));
        rejected(row("fixture-vpn", "127.0.0.1", profile));
        rejected(row("fixture-vpn", "100.64.0.1", profile));
        rejected(row("fixture-vpn", "8.8.8.8", profile.replace("tcp-client", "udp")));
        rejected(row("fixture-vpn", "8.8.8.8", profile.replace("remote 8.8.8.8", "remote 1.1.1.1")));
        rejected(row("fixture-vpn", "8.8.8.8", profile + "remote 8.8.8.8 443\n"));
        rejected(row("fixture-vpn", "8.8.8.8", profile + "<ca>\n" + ca + "\n</ca>"));
        rejected("\"unclosed");
        rejected(csv + ",extra");
        rejected("x".repeat(2 * 1024 * 1024 + 1));
        System.out.println("PASS offline VPN Gate catalogue: strict endpoints, duplicates, profile bounds and separate SSTP port");
    }
}
