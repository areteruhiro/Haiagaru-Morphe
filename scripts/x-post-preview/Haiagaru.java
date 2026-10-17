package app.morphe.extension.chmate;
// Isolated app_process fixture. Never packaged in the extension.
public final class Haiagaru {
    static android.content.Context context;
    public static android.content.Context applicationContextForExtension() { return context; }
    static boolean enabled;
    static boolean full = true;
    static boolean collapsible;
    public static boolean xPostPreviewEnabled() { return enabled; }
    public static boolean xPostPreviewFullText() { return full; }
    public static int mediaAutoFetchMode() { return 3; }
    public static boolean xPostPreviewCollapsible() { return collapsible; }
}
