package app.morphe.extension.chmate;

/** Adds the applied bundle version only to ChMate's explicit device-info text. */
public final class PostVersionInfo {
    private PostVersionInfo() {}

    public static String append(String original) {
        if (original == null || !original.startsWith("2chMate ")
                || original.contains("/Haiagaru ")) return original;
        return original + "/Haiagaru " + BuildConfig.HAIAGARU_VERSION;
    }
}
