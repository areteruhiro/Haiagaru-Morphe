import app.morphe.extension.chmate.ReadCountEvents;
import java.util.ArrayList;
import java.util.List;

/** Run with the compiled ReadCountEvents helper on the class path. */
public class VerifyReadCountEvents {
    public interface Flow { Object collect(Collector collector, Object continuation); }
    public interface Collector { Object emit(Object value, Object continuation); }

    public static void main(String[] args) {
        Object unit = new Object();
        Object continuation = new Object();
        Object suspended = new Object();
        List<Object> emitted = new ArrayList<>();
        Flow upstream = (collector, cont) -> {
            if (cont != continuation) throw new AssertionError("collect continuation changed");
            for (int i = 0; i < 3; i++) {
                if (collector.emit(unit, cont) != suspended) throw new AssertionError("suspended marker changed");
            }
            return suspended;
        };
        Flow adapted = (Flow) ReadCountEvents.distinct(upstream, Flow.class.getName());
        Object result = adapted.collect((value, cont) -> {
            if (cont != continuation) throw new AssertionError("emit continuation changed");
            emitted.add(value);
            return suspended;
        }, continuation);
        if (result != suspended || emitted.size() != 3) throw new AssertionError("emissions lost");
        Object last = unit;
        int recomputations = 0;
        for (Object event : emitted) {
            if (event.equals(last)) throw new AssertionError("stateIn would suppress read event");
            last = event;
            recomputations++;
        }
        if (recomputations != 3) throw new AssertionError("unread count remained stale");
        // Wrapping the unread subscriber must not change the upstream's Unit contract.
        upstream.collect((value, cont) -> {
            if (value != unit) throw new AssertionError("original subscriber changed");
            return suspended;
        }, continuation);
        RuntimeException failure = new RuntimeException("upstream failure");
        Flow failing = (collector, cont) -> { throw failure; };
        try {
            ((Flow) ReadCountEvents.distinct(failing, Flow.class.getName())).collect(null, continuation);
            throw new AssertionError("failure swallowed");
        } catch (RuntimeException actual) {
            if (actual != failure) throw new AssertionError("failure wrapped", actual);
        }
        if (!adapted.equals(adapted) || adapted.equals(upstream)) throw new AssertionError("proxy identity");
        System.out.println("Unread event regression: all checks passed");
    }
}
