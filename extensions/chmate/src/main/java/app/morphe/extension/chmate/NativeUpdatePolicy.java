package app.morphe.extension.chmate;

import android.os.SystemClock;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;

/** Narrow, reversible safeguards for the verified 191 standard DAT/subject reader. */
public final class NativeUpdatePolicy {
    private static final UpdateProgressGate PROGRESS = new UpdateProgressGate();
    private static final Map<Object, Socket> READY = new IdentityHashMap<>();
    private static final ScheduledExecutorService CLEANUP = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
                @Override public Thread newThread(Runnable task) {
                    Thread thread = new Thread(task, "Haiagaru-TCP-cleanup");
                    thread.setDaemon(true);
                    return thread;
                }
            });
    private NativeUpdatePolicy() { }

    /** Replaced with the verified native callback bridge, only in 191. */
    public static void report191(Object listener, int current, int total) { }

    /** Replaced with native Call.timeout access; must run before execute(). */
    public static void configure191(Object call) { }

    public static boolean shouldReport(Object listener, int current, int total) {
        return !Haiagaru.optimize191UpdateProgress()
                || PROGRESS.allow(listener, current, total, SystemClock.uptimeMillis());
    }

    public static long timeoutNanos(String method, String url) {
        int seconds = Haiagaru.update191TimeoutSeconds();
        if (seconds == 0 || !isThreadResource(method, url)) return 0;
        return TimeUnit.SECONDS.toNanos(seconds);
    }

    // These accessors are bound to the verified 191 native types by the patch.
    public static Object call191(Object finder) { return null; }
    public static String method191(Object call) { return null; }
    public static String url191(Object call) { return null; }
    public static boolean cancelled191(Object call) { return false; }
    public static boolean direct191(Object route) { return false; }
    public static List<Object> candidates191(Object finder, Object route) { return null; }
    public static InetSocketAddress endpoint191(Object route) { return null; }
    public static SocketFactory socketFactory191(Object route) { return null; }
    public static void commitRoute191(Object finder, Object original, Object route) { }

    public static Object race191(Object finder, Object original, int timeout) throws IOException {
        if (!Haiagaru.fast191UpdateConnect() || !direct191(original)) return original;
        final Object call = call191(finder);
        if (!isThreadResource(method191(call), url191(call))) return original;
        List<Object> candidates = candidates191(finder, original);
        final List<Object> routes = new ArrayList<>();
        List<InetSocketAddress> addresses = new ArrayList<>();
        for (Object route : candidates) {
            InetSocketAddress address = endpoint191(route);
            if (direct191(route) && !address.isUnresolved() && !addresses.contains(address)) {
                routes.add(route);
                addresses.add(address);
            }
        }
        if (routes.size() < 2) return original;
        TcpConnectRace.Result result = TcpConnectRace.connect(addresses, socketFactory191(original), timeout,
                new TcpConnectRace.Cancellation() {
                    @Override public boolean cancelled() { return cancelled191(call); }
                });
        if (result == null) return original;
        final Object winner = routes.get(result.index);
        final Socket socket = result.socket;
        try {
            if (cancelled191(call)) throw new InterruptedIOException("TCP connection cancelled");
            commitRoute191(finder, original, winner);
            synchronized (READY) { TcpConnectRace.close(READY.put(winner, socket)); }
            CLEANUP.schedule(new Runnable() {
                @Override public void run() {
                    synchronized (READY) {
                        if (READY.get(winner) == socket) TcpConnectRace.close(READY.remove(winner));
                    }
                }
            }, 5, TimeUnit.SECONDS);
            return winner;
        } catch (IOException | RuntimeException error) {
            synchronized (READY) { if (READY.get(winner) == socket) READY.remove(winner); }
            TcpConnectRace.close(socket);
            throw error;
        }
    }

    public static Socket takeSocket191(Object route, Object call) throws IOException {
        Socket socket;
        synchronized (READY) { socket = READY.remove(route); }
        if (socket != null && cancelled191(call)) {
            TcpConnectRace.close(socket);
            throw new InterruptedIOException("TCP connection cancelled");
        }
        return socket;
    }

    static boolean isThreadResource(String method, String url) {
        return UpdateConnectPolicy.isThreadResource(method, url);
    }
}
