package app.morphe.extension.chmate;

import java.util.HashMap;
import java.util.List;

/** Response identities in the original display order, not absolute adapter indexes. */
final class FilterPositionAnchor {
    private FilterPositionAnchor() {}

    static int nearest(List<?> before, List<?> after, int anchor) {
        if (anchor < 0 || anchor >= before.size()) return -1;
        HashMap<Object, Integer> positions = new HashMap<>();
        for (int i = 0; i < after.size(); i++) {
            Object key = after.get(i);
            if (key != null && !positions.containsKey(key)) positions.put(key, i);
        }
        for (int distance = 0; distance < before.size(); distance++) {
            for (int index : new int[]{anchor + distance, anchor - distance}) {
                if (index < 0 || index >= before.size()) continue;
                Object key = before.get(index);
                if (key != null && positions.containsKey(key)) return positions.get(key);
            }
        }
        return -1;
    }
}
