package app.morphe.extension.chmate;

import app.morphe.extension.chmate.vpn.nativecore.imagevpn.Image;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/** Capability-addressed image relay. No URL/query-based public forwarding endpoint. */
public final class ImageVpnRelay implements Closeable {
    private static final int MAX_ENTRIES = 128;
    private static final long TTL_NANOS = TimeUnit.MINUTES.toNanos(10);
    private final ImageVpnFetchManager manager;
    private final ServerSocket listener;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final Set<Socket> clients = Collections.newSetFromMap(new ConcurrentHashMap<Socket, Boolean>());
    private final ScheduledThreadPoolExecutor deadlines = new ScheduledThreadPoolExecutor(1,
            runnable -> daemon(runnable, "HaiagaruImageVpnDeadline"));
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8), runnable -> daemon(runnable, "HaiagaruImageVpnFetch"),
            new ThreadPoolExecutor.AbortPolicy());
    private volatile boolean closed;

    private static final class Entry {
        final String url;
        final long created = System.nanoTime();
        Entry(String url) { this.url = url; }
        boolean expired() { return System.nanoTime() - created >= TTL_NANOS; }
    }

    public ImageVpnRelay(ImageVpnFetchManager manager) throws IOException {
        this.manager = manager;
        deadlines.setRemoveOnCancelPolicy(true);
        listener = new ServerSocket(0, 8, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
        daemon(this::accept, "HaiagaruImageVpnRelay").start();
    }

    public int port() { return listener.getLocalPort(); }

    /** No network access. Native loaders receive this opaque local URL, never the target URL. */
    public synchronized String register(String target) throws IOException {
        if (closed) throw new IOException("IMAGE_VPN_DISABLED");
        if (!ImageVpnRequestPolicy.isProtectedUrl(target)) throw new IOException("IMAGE_VPN_INVALID_IMAGE_URL");
        Iterator<Entry> expired = entries.values().iterator();
        while (expired.hasNext()) if (expired.next().expired()) expired.remove();
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            if (entry.getValue().url.equals(target)) return localUrl(entry.getKey());
        }
        while (entries.size() >= MAX_ENTRIES) {
            Iterator<String> iterator = entries.keySet().iterator();
            iterator.next(); iterator.remove();
        }
        byte[] capability = new byte[32];
        random.nextBytes(capability);
        StringBuilder token = new StringBuilder(64);
        for (byte value : capability) token.append(Character.forDigit((value >>> 4) & 15, 16))
                .append(Character.forDigit(value & 15, 16));
        String path = "/image/" + token + ".jpg";
        entries.put(path, new Entry(target));
        return localUrl(path);
    }

    synchronized String imageIdentity(String value) {
        try {
            java.net.URL url = new java.net.URL(value);
            if (!"127.0.0.1".equals(url.getHost()) || url.getPort() != port()) return value;
            Entry entry = entries.get(url.getPath());
            return entry == null ? value : entry.url;
        } catch (java.net.MalformedURLException invalid) { return value; }
    }

    private String localUrl(String path) { return "http://127.0.0.1:" + port() + path; }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name); thread.setDaemon(true); return thread;
    }

    private void accept() {
        while (!closed) {
            Socket socket = null;
            try {
                socket = listener.accept();
                socket.setSoTimeout(3000);
                clients.add(socket);
                final Socket client = socket;
                final ScheduledFuture<?> deadline = deadlines.schedule(() -> release(client), 120, TimeUnit.SECONDS);
                try {
                    workers.execute(() -> {
                        try { handle(client); } finally { deadline.cancel(false); }
                    });
                } catch (Exception rejected) { deadline.cancel(false); throw rejected; }
            } catch (Exception failure) {
                if (socket != null) release(socket);
                // Closed/busy loopback connections never redirect to the upstream image.
                if (listener.isClosed()) return;
            }
        }
    }

    private void handle(Socket socket) {
        try {
            InputStream input = socket.getInputStream();
            int[] remaining = {8192};
            String[] request = line(input, remaining).split(" ", -1);
            if (request.length != 3 || !request[0].equals("GET")
                    || !(request[2].equals("HTTP/1.1") || request[2].equals("HTTP/1.0"))) {
                error(socket, 400); return;
            }
            String host = null;
            for (int count = 0; ; count++) {
                String header = line(input, remaining);
                if (header.isEmpty()) break;
                if (count >= 64) throw new IOException("IMAGE_VPN_HEADERS_TOO_LARGE");
                int separator = header.indexOf(':');
                if (separator < 1) { error(socket, 400); return; }
                String name = header.substring(0, separator).trim();
                if (name.equalsIgnoreCase("Host")) {
                    if (host != null) { error(socket, 400); return; }
                    host = header.substring(separator + 1).trim();
                }
                // Requests have no bodies; reject ambiguous framing and proxy semantics.
                if (name.equalsIgnoreCase("Transfer-Encoding") || name.equalsIgnoreCase("Content-Length")) {
                    error(socket, 400); return;
                }
            }
            if (!("127.0.0.1:" + port()).equals(host)) { error(socket, 403); return; }
            Entry entry;
            synchronized (this) { entry = closed ? null : entries.get(request[1]); }
            if (entry == null || entry.expired()) { error(socket, 410); return; }
            Image image = manager.fetch(entry.url);
            if (closed) { error(socket, 503); return; }
            byte[] body = image.getBody();
            String type = image.getContentType();
            if (body == null || body.length > 32 * 1024 * 1024
                    || type == null || !type.matches("image/[a-zA-Z0-9.+-]+")) {
                error(socket, 502); return;
            }
            response(socket, 200, type, body);
        } catch (Exception | LinkageError failure) {
            try { error(socket, 503); } catch (IOException ignored) { }
        } finally { release(socket); }
    }

    private static String line(InputStream input, int[] remaining) throws IOException {
        StringBuilder result = new StringBuilder();
        while (--remaining[0] >= 0) {
            int value = input.read();
            if (value < 0) throw new IOException("IMAGE_VPN_HEADER_EOF");
            if (value == '\r') {
                if (--remaining[0] < 0 || input.read() != '\n') throw new IOException("IMAGE_VPN_INVALID_HEADERS");
                return result.toString();
            }
            if (value < 32 || value > 126) throw new IOException("IMAGE_VPN_INVALID_HEADERS");
            result.append((char)value);
        }
        throw new IOException("IMAGE_VPN_HEADERS_TOO_LARGE");
    }

    private static void error(Socket socket, int status) throws IOException {
        response(socket, status, "text/plain", "IMAGE_VPN_UNAVAILABLE".getBytes(StandardCharsets.US_ASCII));
    }

    private static void response(Socket socket, int status, String type, byte[] body) throws IOException {
        OutputStream output = socket.getOutputStream();
        String headers = "HTTP/1.1 " + status + " ImageRelay\r\nContent-Type: " + type
                + "\r\nContent-Length: " + body.length + "\r\nConnection: close\r\nCache-Control: no-store"
                + "\r\nX-Content-Type-Options: nosniff\r\n\r\n";
        output.write(headers.getBytes(StandardCharsets.US_ASCII));
        output.write(body); output.flush();
    }

    private void release(Socket socket) {
        clients.remove(socket);
        try { socket.close(); } catch (IOException ignored) { }
    }

    @Override public void close() {
        synchronized (this) { closed = true; entries.clear(); }
        try { listener.close(); } catch (IOException ignored) { }
        workers.shutdownNow();
        deadlines.shutdownNow();
        manager.close();
        for (Socket client : clients) release(client);
    }
}
