package app.morphe.extension.chmate;

import java.util.Arrays;
import java.util.List;

public class VerifyFilterPositionAnchor {
    private static void check(List<?> before, List<?> after, int anchor, int expected) {
        int actual = FilterPositionAnchor.nearest(before, after, anchor);
        if (actual != expected) throw new AssertionError(before + " -> " + after + ": " + actual + " != " + expected);
    }
    public static void main(String[] args) {
        List<Integer> original = Arrays.asList(null, 1, 2, 3, 4, 5);
        check(original, Arrays.asList(null, 1, 4, 5), 4, 2); // Same response, different adapter index.
        check(original, Arrays.asList(null, 1, 5), 3, 2); // Forward wins an equal-distance tie.
        check(original, Arrays.asList(null, 1), 5, 1); // Last matching response before the anchor.
        check(original, Arrays.asList(null, null), 3, -1); // Empty response set: do not jump.
        check(original, original, 3, 3);
        check(Arrays.asList(9, 3, 8, 1, 7), Arrays.asList(9, 1, 7), 2, 1); // Custom order, not numeric order.
        check(Arrays.asList(3, 3, 4), Arrays.asList(3, 3, 4), 1, 0); // Deterministic duplicate identity.
        check(original, original, -1, -1);
        check(original, original, 6, -1);
        System.out.println("PASS: 9 filter-anchor cases");
    }
}
