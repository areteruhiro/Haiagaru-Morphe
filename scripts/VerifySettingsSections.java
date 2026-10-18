package app.morphe.extension.chmate;

public final class VerifySettingsSections {
    static int checks;
    static void check(boolean result) { if (!result) throw new AssertionError("case " + checks); checks++; }
    public static void main(String[] args) {
        check(SettingsSections.Category.values().length == 12);
        check(SettingsSections.Category.values()[0] == SettingsSections.Category.GENERAL);
        check(SettingsSections.Category.values()[11] == SettingsSections.Category.ADVANCED);
        check(SettingsSections.matches("画像・動画・X／Twitter", "twitter"));
        check(SettingsSections.matches("画像・動画・X／Twitter", "ＴＷＩＴＴＥＲ"));
        check(SettingsSections.matches("画像・動画・X／Twitter", "画像 twitter"));
        check(!SettingsSections.matches("画像・動画・X／Twitter", "画像 履歴"));
        check(SettingsSections.matches("フィルタ NG ＩＤ", "ng id"));
        check(SettingsSections.matches("履歴・保存件数", "　履歴　"));
        check(SettingsSections.matches("User-Agent MonaKey HTTPS", "user-agent https"));
        check(!SettingsSections.matches("履歴・保存件数", "VPN"));
        check(SettingsSections.matches("", ""));
        check(!SettingsSections.matches("", "画像"));
        check(SettingsSections.matches("abc", "   "));
        System.out.println("PASS " + checks + " settings category/search cases");
    }
}
