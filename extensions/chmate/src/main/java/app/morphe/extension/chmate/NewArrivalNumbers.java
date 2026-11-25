package app.morphe.extension.chmate;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public final class NewArrivalNumbers {
    static final String KEY = "countNewArrivalResponseNumbers";
    static boolean enabled() { return FeatureSwitches.enabled(KEY, true); }
    private static final Map<Object, Snapshot> SNAPSHOTS = new WeakHashMap<>();
    private static final class Snapshot {
        final int last;
        Set<Integer> current;
        Snapshot(int last) { this.last = last; }
    }
    private static Set<Integer> numbers(Collection<?> rows) throws Exception {
        Set<Integer> numbers = new HashSet<>();
        for (Object row : PopularReplies.fullList(rows)) {
            if (row == null) continue;
            Field number = NativeFields.find(row.getClass(), row.getClass().getName().equals("o.BouncyCastleSocketAdapterCompanion") ? "l" : "o");
            int value = ((Number) number.get(row)).intValue();
            if (value > 0) numbers.add(value);
        }
        return numbers;
    }
    public static int before(Object owner, Collection<?> rows) {
        synchronized (SNAPSHOTS) {
            SNAPSHOTS.remove(owner);
            if (!enabled()) return rows.size();
            try {
                int last = 0;
                for (int number : numbers(rows)) last = Math.max(last, number);
                SNAPSHOTS.put(owner, new Snapshot(last));
            } catch (Exception unavailable) { }
        }
        return rows.size();
    }
    public static void after(Object owner, Collection<?> rows) {
        synchronized (SNAPSHOTS) {
            Snapshot snapshot = SNAPSHOTS.get(owner);
            if (snapshot == null) return;
            try { snapshot.current = numbers(rows); }
            catch (Exception unavailable) { snapshot.current = null; }
        }
    }
    public static int count(Object owner, int previous, int current) {
        Snapshot snapshot;
        synchronized (SNAPSHOTS) { snapshot = SNAPSHOTS.remove(owner); }
        if (!enabled() || snapshot == null || snapshot.current == null) return HiddenNgUnread.newArrivalCount(previous, current);
        Set<Integer> added = new HashSet<>();
        for (int number : snapshot.current) if (number > snapshot.last) added.add(number);
        return HiddenNgUnread.numberedNewArrivalCount(added);
    }
    private NewArrivalNumbers() { }
}
