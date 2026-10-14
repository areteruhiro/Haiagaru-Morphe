package app.morphe.extension.chmate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpRetryException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

/** Platform redirects are disabled: every destination is routed before DNS/connect. */
public final class ImageVpnRedirectConnection {
    private ImageVpnRedirectConnection() {}

    public static URLConnection wrap(URLConnection connection, URL logical, Proxy proxy,
            boolean explicitProxy) {
        if (!(connection instanceof HttpURLConnection)) return connection;
        State state = new State((HttpURLConnection) connection, logical, proxy, explicitProxy);
        Plain plain = new Plain(state);
        return connection instanceof HttpsURLConnection ? new Secure(plain) : plain;
    }

    private static final class State {
        HttpURLConnection active;
        URL logical;
        final Proxy proxy;
        final boolean explicitProxy;
        boolean follow = HttpURLConnection.getFollowRedirects();
        boolean responded, streaming, outputRequested;
        IOException failure;
        ByteArrayOutputStream body;
        Map<String, List<String>> headers;
        SSLSocketFactory sslFactory;
        HostnameVerifier verifier;

        State(HttpURLConnection connection, URL logical, Proxy proxy, boolean explicitProxy) {
            this.active = connection; this.logical = logical; this.proxy = proxy;
            this.explicitProxy = explicitProxy;
            connection.setInstanceFollowRedirects(false);
            if (connection instanceof HttpsURLConnection) {
                sslFactory = ((HttpsURLConnection) connection).getSSLSocketFactory();
                verifier = ((HttpsURLConnection) connection).getHostnameVerifier();
            }
        }

        void snapshot() {
            try { headers = new LinkedHashMap<>(active.getRequestProperties()); }
            catch (IllegalStateException connected) { /* connect() captured these already. */ }
        }

        synchronized int response() throws IOException {
            if (failure != null) throw failure;
            if (responded) return active.getResponseCode();
            try {
                snapshot();
                if (body != null) {
                    try (OutputStream output = active.getOutputStream()) { body.writeTo(output); }
                }
                for (int redirects = 0; ; redirects++) {
                    int code = active.getResponseCode();
                    String location = active.getHeaderField("Location");
                    if (!follow || location == null || !(code == 301 || code == 302 || code == 303
                            || code == 307 || code == 308)) {
                        responded = true; body = null; return code;
                    }
                    if (redirects >= 20) throw new ProtocolException("IMAGE_VPN_REDIRECT_LIMIT");
                    URL next = new URL(logical, location);
                    if (!("http".equalsIgnoreCase(next.getProtocol())
                            || "https".equalsIgnoreCase(next.getProtocol()))) {
                        responded = true; return code;
                    }
                    String method = active.getRequestMethod();
                    boolean toGet = ((code == 301 || code == 302) && "POST".equals(method))
                            || (code == 303 && !"HEAD".equals(method));
                    // Streaming bodies cannot safely be replayed (same as the platform API).
                    if (streaming && outputRequested)
                        throw new HttpRetryException("Cannot retry streamed request", code, location);
                    boolean sameOrigin = sameOrigin(logical, next);
                    HttpURLConnection previous = active;
                    // An in-flight opt-in request never falls back to a direct target after disable.
                    if (ImageVpnRequestPolicy.isTarget(next) && !ImageVpnController.enabled())
                        throw new IOException("IMAGE_VPN_DISABLED_DURING_REDIRECT");
                    HttpURLConnection replacement = (HttpURLConnection)
                            HttpsTransport.openRawConnection(next, proxy, explicitProxy);
                    replacement.setInstanceFollowRedirects(false);
                    replacement.setConnectTimeout(previous.getConnectTimeout());
                    replacement.setReadTimeout(previous.getReadTimeout());
                    replacement.setUseCaches(previous.getUseCaches());
                    replacement.setAllowUserInteraction(previous.getAllowUserInteraction());
                    replacement.setIfModifiedSince(previous.getIfModifiedSince());
                    replacement.setDoInput(previous.getDoInput());
                    replacement.setDoOutput(!toGet && previous.getDoOutput());
                    replacement.setRequestMethod(toGet ? "GET" : method);
                    if (headers != null) for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                        String name = entry.getKey();
                        if (name == null || name.equalsIgnoreCase("Host")) continue;
                        if (!sameOrigin && (name.equalsIgnoreCase("Authorization")
                                || name.equalsIgnoreCase("Proxy-Authorization") || name.equalsIgnoreCase("Cookie")
                                || name.equalsIgnoreCase("Cookie2"))) continue;
                        if (toGet && (name.equalsIgnoreCase("Content-Length")
                                || name.equalsIgnoreCase("Content-Type") || name.equalsIgnoreCase("Transfer-Encoding"))) continue;
                        for (String value : entry.getValue()) replacement.addRequestProperty(name, value);
                    }
                    if (replacement instanceof HttpsURLConnection) {
                        if (sslFactory != null) ((HttpsURLConnection) replacement).setSSLSocketFactory(sslFactory);
                        if (verifier != null) ((HttpsURLConnection) replacement).setHostnameVerifier(verifier);
                    }
                    previous.disconnect(); active = replacement; logical = next;
                    snapshot();
                    if (toGet) body = null;
                    if (body != null) try (OutputStream output = active.getOutputStream()) { body.writeTo(output); }
                }
            } catch (IOException error) {
                failure = error; active.disconnect(); throw error;
            }
        }

        static boolean sameOrigin(URL a, URL b) {
            int ap = a.getPort() == -1 ? a.getDefaultPort() : a.getPort();
            int bp = b.getPort() == -1 ? b.getDefaultPort() : b.getPort();
            return a.getProtocol().equalsIgnoreCase(b.getProtocol())
                    && a.getHost().equalsIgnoreCase(b.getHost()) && ap == bp;
        }

        OutputStream output() throws IOException {
            if (responded || failure != null) throw new ProtocolException("Request already completed");
            if (!active.getDoOutput()) throw new ProtocolException("doOutput is false");
            outputRequested = true;
            if ("GET".equals(active.getRequestMethod())) active.setRequestMethod("POST");
            snapshot();
            if (streaming) return active.getOutputStream();
            // The platform also buffers non-streaming bodies. Keep them for 307/308 replay.
            if (body == null) body = new ByteArrayOutputStream();
            return body;
        }

        void readHeaders() { try { response(); } catch (IOException ignored) { } }
    }

    private static final class Plain extends HttpURLConnection {
        final State s;
        Plain(State state) { super(state.logical); s = state; }
        @Override public void connect() throws IOException { s.snapshot(); s.active.connect(); connected = true; }
        @Override public void disconnect() { s.active.disconnect(); }
        @Override public boolean usingProxy() { return s.active.usingProxy(); }
        @Override public URL getURL() { return s.logical; }
        @Override public InputStream getInputStream() throws IOException { s.response(); return s.active.getInputStream(); }
        @Override public OutputStream getOutputStream() throws IOException { return s.output(); }
        @Override public InputStream getErrorStream() { return s.active.getErrorStream(); }
        @Override public int getResponseCode() throws IOException { return s.response(); }
        @Override public String getResponseMessage() throws IOException { s.response(); return s.active.getResponseMessage(); }
        @Override public String getHeaderField(String name) { s.readHeaders(); return s.failure == null ? s.active.getHeaderField(name) : null; }
        @Override public String getHeaderField(int n) { s.readHeaders(); return s.failure == null ? s.active.getHeaderField(n) : null; }
        @Override public String getHeaderFieldKey(int n) { s.readHeaders(); return s.failure == null ? s.active.getHeaderFieldKey(n) : null; }
        @Override public Map<String, List<String>> getHeaderFields() { s.readHeaders(); return s.failure == null ? s.active.getHeaderFields() : java.util.Collections.emptyMap(); }
        @Override public void setInstanceFollowRedirects(boolean value) { s.follow = value; }
        @Override public boolean getInstanceFollowRedirects() { return s.follow; }
        @Override public void setRequestMethod(String method) throws ProtocolException { s.active.setRequestMethod(method); }
        @Override public String getRequestMethod() { return s.active.getRequestMethod(); }
        @Override public void setRequestProperty(String key, String value) { s.active.setRequestProperty(key, value); }
        @Override public void addRequestProperty(String key, String value) { s.active.addRequestProperty(key, value); }
        @Override public String getRequestProperty(String key) { return s.active.getRequestProperty(key); }
        @Override public Map<String, List<String>> getRequestProperties() { return s.active.getRequestProperties(); }
        @Override public void setConnectTimeout(int value) { s.active.setConnectTimeout(value); }
        @Override public int getConnectTimeout() { return s.active.getConnectTimeout(); }
        @Override public void setReadTimeout(int value) { s.active.setReadTimeout(value); }
        @Override public int getReadTimeout() { return s.active.getReadTimeout(); }
        @Override public void setDoInput(boolean value) { s.active.setDoInput(value); }
        @Override public boolean getDoInput() { return s.active.getDoInput(); }
        @Override public void setDoOutput(boolean value) { s.active.setDoOutput(value); }
        @Override public boolean getDoOutput() { return s.active.getDoOutput(); }
        @Override public void setUseCaches(boolean value) { s.active.setUseCaches(value); }
        @Override public boolean getUseCaches() { return s.active.getUseCaches(); }
        @Override public void setDefaultUseCaches(boolean value) { s.active.setDefaultUseCaches(value); }
        @Override public boolean getDefaultUseCaches() { return s.active.getDefaultUseCaches(); }
        @Override public void setIfModifiedSince(long value) { s.active.setIfModifiedSince(value); }
        @Override public long getIfModifiedSince() { return s.active.getIfModifiedSince(); }
        @Override public void setAllowUserInteraction(boolean value) { s.active.setAllowUserInteraction(value); }
        @Override public boolean getAllowUserInteraction() { return s.active.getAllowUserInteraction(); }
        @Override public Permission getPermission() throws IOException { return s.active.getPermission(); }
        @Override public void setFixedLengthStreamingMode(int length) { s.active.setFixedLengthStreamingMode(length); s.streaming = true; }
        @Override public void setFixedLengthStreamingMode(long length) { s.active.setFixedLengthStreamingMode(length); s.streaming = true; }
        @Override public void setChunkedStreamingMode(int length) { s.active.setChunkedStreamingMode(length); s.streaming = true; }
    }

    /** Preserve callers' HttpsURLConnection casts and configured TLS validation. */
    private static final class Secure extends HttpsURLConnection {
        final Plain p;
        Secure(Plain plain) { super(plain.getURL()); p = plain; }
        HttpsURLConnection tls() throws IOException {
            if (!(p.s.active instanceof HttpsURLConnection)) throw new IOException("Connection is no longer HTTPS");
            return (HttpsURLConnection) p.s.active;
        }
        @Override public String getCipherSuite() { try { return tls().getCipherSuite(); } catch (IOException e) { throw new IllegalStateException(e); } }
        @Override public Certificate[] getLocalCertificates() { try { return tls().getLocalCertificates(); } catch (IOException e) { throw new IllegalStateException(e); } }
        @Override public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
            try { return tls().getServerCertificates(); } catch (SSLPeerUnverifiedException e) { throw e; }
            catch (IOException e) { throw new IllegalStateException(e); }
        }
        @Override public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
            try { return tls().getPeerPrincipal(); } catch (SSLPeerUnverifiedException e) { throw e; }
            catch (IOException e) { throw new IllegalStateException(e); }
        }
        @Override public Principal getLocalPrincipal() { try { return tls().getLocalPrincipal(); } catch (IOException e) { throw new IllegalStateException(e); } }
        @Override public void setSSLSocketFactory(SSLSocketFactory value) { ((HttpsURLConnection) p.s.active).setSSLSocketFactory(value); p.s.sslFactory = value; }
        @Override public SSLSocketFactory getSSLSocketFactory() { return p.s.sslFactory; }
        @Override public void setHostnameVerifier(HostnameVerifier value) { ((HttpsURLConnection) p.s.active).setHostnameVerifier(value); p.s.verifier = value; }
        @Override public HostnameVerifier getHostnameVerifier() { return p.s.verifier; }
        @Override public void connect() throws IOException { p.connect(); }
        @Override public void disconnect() { p.disconnect(); }
        @Override public boolean usingProxy() { return p.usingProxy(); }
        @Override public URL getURL() { return p.getURL(); }
        @Override public InputStream getInputStream() throws IOException { return p.getInputStream(); }
        @Override public OutputStream getOutputStream() throws IOException { return p.getOutputStream(); }
        @Override public InputStream getErrorStream() { return p.getErrorStream(); }
        @Override public int getResponseCode() throws IOException { return p.getResponseCode(); }
        @Override public String getResponseMessage() throws IOException { return p.getResponseMessage(); }
        @Override public String getHeaderField(String name) { return p.getHeaderField(name); }
        @Override public String getHeaderField(int n) { return p.getHeaderField(n); }
        @Override public String getHeaderFieldKey(int n) { return p.getHeaderFieldKey(n); }
        @Override public Map<String, List<String>> getHeaderFields() { return p.getHeaderFields(); }
        @Override public void setInstanceFollowRedirects(boolean value) { p.setInstanceFollowRedirects(value); }
        @Override public boolean getInstanceFollowRedirects() { return p.getInstanceFollowRedirects(); }
        @Override public void setRequestMethod(String value) throws ProtocolException { p.setRequestMethod(value); }
        @Override public String getRequestMethod() { return p.getRequestMethod(); }
        @Override public void setRequestProperty(String key, String value) { p.setRequestProperty(key, value); }
        @Override public void addRequestProperty(String key, String value) { p.addRequestProperty(key, value); }
        @Override public String getRequestProperty(String key) { return p.getRequestProperty(key); }
        @Override public Map<String, List<String>> getRequestProperties() { return p.getRequestProperties(); }
        @Override public void setConnectTimeout(int value) { p.setConnectTimeout(value); }
        @Override public int getConnectTimeout() { return p.getConnectTimeout(); }
        @Override public void setReadTimeout(int value) { p.setReadTimeout(value); }
        @Override public int getReadTimeout() { return p.getReadTimeout(); }
        @Override public void setDoInput(boolean value) { p.setDoInput(value); }
        @Override public boolean getDoInput() { return p.getDoInput(); }
        @Override public void setDoOutput(boolean value) { p.setDoOutput(value); }
        @Override public boolean getDoOutput() { return p.getDoOutput(); }
        @Override public void setUseCaches(boolean value) { p.setUseCaches(value); }
        @Override public boolean getUseCaches() { return p.getUseCaches(); }
        @Override public void setDefaultUseCaches(boolean value) { p.setDefaultUseCaches(value); }
        @Override public boolean getDefaultUseCaches() { return p.getDefaultUseCaches(); }
        @Override public void setIfModifiedSince(long value) { p.setIfModifiedSince(value); }
        @Override public long getIfModifiedSince() { return p.getIfModifiedSince(); }
        @Override public void setAllowUserInteraction(boolean value) { p.setAllowUserInteraction(value); }
        @Override public boolean getAllowUserInteraction() { return p.getAllowUserInteraction(); }
        @Override public Permission getPermission() throws IOException { return p.getPermission(); }
        @Override public void setFixedLengthStreamingMode(int value) { p.setFixedLengthStreamingMode(value); }
        @Override public void setFixedLengthStreamingMode(long value) { p.setFixedLengthStreamingMode(value); }
        @Override public void setChunkedStreamingMode(int value) { p.setChunkedStreamingMode(value); }
    }
}
