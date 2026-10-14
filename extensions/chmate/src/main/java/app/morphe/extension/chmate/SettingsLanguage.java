package app.morphe.extension.chmate;

import java.util.Locale;

/** Haiagaru's own text selection, independent of Android's per-app locale APIs. */
public final class SettingsLanguage {
    private SettingsLanguage() {}

    public static int index(String value) {
        if ("en".equals(value)) return 1;
        if ("system".equals(value)) return 2;
        return 0;
    }

    public static String value(int index) {
        return index == 1 ? "en" : index == 2 ? "system" : "ja";
    }

    public static String text(String mode, Locale systemLocale, String japanese, String english) {
        int choice = index(mode);
        boolean useJapanese = choice == 0 || (choice == 2 && systemLocale != null
                && "ja".equals(systemLocale.getLanguage()));
        return useJapanese ? japanese : english;
    }
}
