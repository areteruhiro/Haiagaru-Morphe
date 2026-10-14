import app.morphe.extension.chmate.ViewerControlInsets;

public final class VerifyViewerControlInsets {
    private static void check(int actual, int expected) {
        if (actual != expected) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        check(ViewerControlInsets.margin(32, 48, 0), 80); // Edge-to-edge / three-button navigation.
        check(ViewerControlInsets.margin(32, 24, 0), 56); // Gesture navigation.
        check(ViewerControlInsets.margin(32, 48, 48), 32); // Host already fits navigation bar.
        check(ViewerControlInsets.margin(32, 48, 64), 32); // Host padding already exceeds inset.
        check(ViewerControlInsets.margin(32, 48, 16), 64); // Partially fitted content.
        check(ViewerControlInsets.margin(24, 48, 0), 72); // Landscape side navigation bar.
        check(ViewerControlInsets.margin(32, 0, 0), 32); // No reported insets / older device.
        check(ViewerControlInsets.margin(32, -1, -1), 32);
        System.out.println("PASS viewer control safe insets and no double counting");
    }
}
