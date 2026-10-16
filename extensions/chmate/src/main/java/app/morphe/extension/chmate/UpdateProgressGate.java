package app.morphe.extension.chmate;

import java.util.WeakHashMap;

/** Per-download UI progress coalescing. Never throttles received data or cancellation. */
public final class UpdateProgressGate {
    private final WeakHashMap<Object, State> states = new WeakHashMap<>();
    private static final long INTERVAL_MS = 100;
    private static final class State {
        int current, total;
        long reported;
    }

    public synchronized boolean allow(Object listener, int current, int total, long now) {
        if (listener == null || current <= 0 || total <= 0 || current >= total) {
            states.remove(listener);
            return true;
        }
        State state = states.get(listener);
        boolean report = state == null || total != state.total || current < state.current
                || now < state.reported || now - state.reported >= INTERVAL_MS;
        if (state == null) {
            state = new State();
            states.put(listener, state);
        }
        state.current = current;
        state.total = total;
        if (report) state.reported = now;
        return report;
    }
}
