import app.morphe.extension.chmate.AutoReloadTitle;

public final class VerifyAutoReloadTitle {
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) {
        check("【自動10s】 スレタイ".equals(AutoReloadTitle.format("スレタイ", 10000)));
        check("【自動30s】 スレタイ".equals(AutoReloadTitle.format("スレタイ", 30000)));
        check("【自動13s】 スレタイ".equals(AutoReloadTitle.format("スレタイ", 13000)));
        check("【自動14s】 スレタイ".equals(AutoReloadTitle.format("スレタイ", 13001)));
        check("スレタイ".equals(AutoReloadTitle.format("スレタイ", 0)));
        check(AutoReloadTitle.format(null, 10000) == null);
        System.out.println("PASS auto-reload title status formatting");
    }
}
