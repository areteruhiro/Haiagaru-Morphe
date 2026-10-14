package app.morphe.extension.chmate;

/** Display-only status prefix; never changes the thread model/title used for copying. */
public final class AutoReloadTitle {
    private AutoReloadTitle() {}
    public static String format(String title, long intervalMillis) {
        if (title == null || intervalMillis <= 0) return title;
        long seconds = intervalMillis / 1000 + (intervalMillis % 1000 == 0 ? 0 : 1);
        return "【自動" + seconds + "s】 " + title;
    }
}
