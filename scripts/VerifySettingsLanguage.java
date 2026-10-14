import app.morphe.extension.chmate.SettingsLanguage;
import java.util.Locale;

public final class VerifySettingsLanguage {
    private static void check(String mode, Locale locale, String expected) {
        String actual = SettingsLanguage.text(mode, locale, "日本語", "English");
        if (!expected.equals(actual)) throw new AssertionError(mode + "/" + locale + ": " + actual);
    }
    public static void main(String[] args) {
        Locale original = Locale.getDefault();
        for (Locale locale : new Locale[]{Locale.JAPAN, Locale.ENGLISH, Locale.US, Locale.FRANCE, Locale.ROOT, null}) {
            check("ja", locale, "日本語");
            check("en", locale, "English");
            check(null, locale, "日本語");
            check("invalid", locale, "日本語");
            check("system", locale, locale != null && "ja".equals(locale.getLanguage()) ? "日本語" : "English");
        }
        for (int index = 0; index < 3; index++) {
            if (SettingsLanguage.index(SettingsLanguage.value(index)) != index) throw new AssertionError("Selection round trip");
        }
        if (!"ja".equals(SettingsLanguage.value(-1)) || !"ja".equals(SettingsLanguage.value(99))) throw new AssertionError("Invalid selection fallback");
        if (!original.equals(Locale.getDefault())) throw new AssertionError("Must not change device/ChMate locale");
        System.out.println("PASS settings language: Japanese default, English, system, fallback and locale isolation");
    }
}
