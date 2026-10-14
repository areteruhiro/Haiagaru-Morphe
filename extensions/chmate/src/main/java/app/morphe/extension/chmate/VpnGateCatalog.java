package app.morphe.extension.chmate;

import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** TLS-authenticated official VPN Gate catalogue. Never evaluates downloaded configuration. */
public final class VpnGateCatalog {
    private static final String CATALOG = "https://www.vpngate.net/api/iphone/";
    private static final Pattern REMOTE = Pattern.compile("(?m)^remote[ \\t]+([0-9.]+)[ \\t]+([0-9]+)[ \\t]*\\r?$");
    private static final Pattern CA = Pattern.compile("(?s)<ca>\\s*(-----BEGIN CERTIFICATE-----.*?-----END CERTIFICATE-----)\\s*</ca>");
    private VpnGateCatalog() {}

    public static final class Server {
        public final String ip, hostname, trustAnchorPem;
        public final int port;
        public final long score;
        private Server(String ip, String hostname, int port, String pem, long score) {
            this.ip = ip; this.hostname = hostname; this.port = port; trustAnchorPem = pem; this.score = score;
        }
    }

    public static List<Server> fetch() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(CATALOG).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(15000); connection.setReadTimeout(15000);
        try {
            if (connection.getResponseCode() != 200) throw new IOException("IMAGE_VPN_CATALOG_HTTP_FAILED");
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            try (InputStream input = connection.getInputStream()) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) != -1) {
                    if (data.size() + count > 2 * 1024 * 1024) throw new IOException("IMAGE_VPN_CATALOG_TOO_LARGE");
                    data.write(buffer, 0, count);
                }
            }
            List<Server> servers = parse(new String(data.toByteArray(), StandardCharsets.UTF_8));
            if (servers.isEmpty()) throw new IOException("IMAGE_VPN_NO_SUPPORTED_SERVER");
            return servers;
        } finally { connection.disconnect(); }
    }

    public static List<Server> parse(String csv) {
        List<Server> servers = new ArrayList<>();
        if (csv == null || csv.length() > 2 * 1024 * 1024) return servers;
        for (String line : csv.split("\\r?\\n")) {
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("*") || line.length() > 65536) continue;
            try {
                List<String> fields = row(line);
                if (fields.size() != 15 || !publicIPv4(fields.get(1))) continue;
                String hostname = fields.get(0).toLowerCase(Locale.ROOT);
                if (!hostname.matches("[a-z0-9-]{1,63}")) continue;
                hostname += ".opengw.net";
                String profile = new String(Base64.decode(fields.get(14), Base64.DEFAULT), StandardCharsets.US_ASCII);
                // Read only the endpoint/trust anchor. No scripts, files or OpenVPN directives execute.
                if (!Pattern.compile("(?m)^proto[ \\t]+tcp(?:-client)?[ \\t]*\\r?$").matcher(profile).find()) continue;
                Matcher remote = REMOTE.matcher(profile), ca = CA.matcher(profile);
                if (!remote.find() || !remote.group(1).equals(fields.get(1)) || !ca.find()) continue;
                int port = Integer.parseInt(remote.group(2));
                if (port < 1 || port > 65535) continue;
                String pem = ca.group(1);
                if (remote.find() || ca.find()) continue;
                if (pem.length() > 16384) continue;
                X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                        .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
                cert.checkValidity();
                // The OpenVPN profile port is NOT evidence of an SSTP listener.
                // Official SSTP instructions specify 443 unless explicitly advertised otherwise.
                // Probe TLS/SSTP on 443; a rejection/unsupported relay is skipped, never downgraded.
                long score = Long.parseLong(fields.get(2));
                if (score < 0 || cert.getBasicConstraints() < 0) continue;
                boolean duplicate = false;
                for (Server existing : servers) if (existing.ip.equals(fields.get(1))) duplicate = true;
                if (!duplicate) servers.add(new Server(fields.get(1), hostname, 443, pem, score));
            } catch (Exception ignored) { /* Malformed/unsupported entry: skip without contacting it. */ }
        }
        servers.sort(Comparator.comparingLong((Server server) -> server.score).reversed());
        return new ArrayList<>(servers.subList(0, Math.min(8, servers.size())));
    }

    private static List<String> row(String line) {
        List<String> values = new ArrayList<>(); StringBuilder value = new StringBuilder(); boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') { value.append('"'); i++; }
                else quoted = !quoted;
            } else if (c == ',' && !quoted) { values.add(value.toString()); value.setLength(0); }
            else value.append(c);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quote");
        values.add(value.toString()); return values;
    }

    private static boolean publicIPv4(String text) {
        String[] parts = text.split("\\."); if (parts.length != 4) return false;
        int[] bytes = new int[4];
        for (int i = 0; i < 4; i++) {
            if (!parts[i].matches("0|[1-9][0-9]{0,2}")) return false;
            bytes[i] = Integer.parseInt(parts[i]); if (bytes[i] > 255) return false;
        }
        int a = bytes[0], b = bytes[1], c = bytes[2];
        return !(a == 0 || a == 10 || a == 127 || a >= 224 || (a == 100 && b >= 64 && b <= 127)
                || (a == 169 && b == 254) || (a == 172 && b >= 16 && b <= 31)
                || (a == 192 && (b == 168 || (b == 0 && (c == 0 || c == 2))))
                || (a == 198 && (b == 18 || b == 19 || (b == 51 && c == 100)))
                || (a == 203 && b == 0 && c == 113));
    }
}
