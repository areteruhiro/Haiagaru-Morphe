package app.morphe.extension.chmate;

import java.util.List;

/** Null is ChMate's native "no NG display bucket" value. Never mutate stored data. */
public final class NgThreadGroup {
    private NgThreadGroup() { }

    public static <T> List<T> visible(List<T> threads, boolean hidden) {
        return hidden ? null : threads;
    }
}
