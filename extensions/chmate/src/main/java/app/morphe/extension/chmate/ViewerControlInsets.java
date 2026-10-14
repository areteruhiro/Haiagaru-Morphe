package app.morphe.extension.chmate;

/** Avoid both overlapping system controls and counting an already-fitted inset twice. */
public final class ViewerControlInsets {
    private ViewerControlInsets() {}
    public static int margin(int gap, int inset, int excludedByHost) {
        return Math.max(0, gap) + Math.max(0, Math.max(0, inset) - Math.max(0, excludedByHost));
    }
}
