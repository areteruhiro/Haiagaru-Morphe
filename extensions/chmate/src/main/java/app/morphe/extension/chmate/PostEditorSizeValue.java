package app.morphe.extension.chmate;

/** Input validation shared by the settings UI and preference reader. */
public final class PostEditorSizeValue {
    private PostEditorSizeValue() {}
    public static float parse(String text) {
        try {
            float value = Float.parseFloat(text.trim());
            return valid(value) ? value : -1;
        } catch (RuntimeException error) { return -1; }
    }
    public static boolean valid(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value) && value >= 1 && value <= 200;
    }
    public static int choice(float value) {
        if (value == 0) return 0;
        return value >= 10 && value <= 36 && value == (int) value ? (int) value - 9 : 28;
    }
}
