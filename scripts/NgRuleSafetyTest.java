package app.morphe.extension.chmate;

public final class NgRuleSafetyTest {
    private static int checks;
    private static void ok(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
    public static void main(String[] args) {
        String example = ".*[\\x{E000}-\\x{F8FF}\\x{10000}-\\x{FFFFF}].*";
        ok(NgRuleSafety.containsPrivateUseRange(example), "reported BMP private-use range is detected");
        ok(NgRuleSafety.containsPrivateUseRange("[\\x{F0000}-\\x{FFFFD}]"), "plane 15 private-use range is detected");
        ok(NgRuleSafety.containsPrivateUseRange("[\\x{100000}-\\x{10FFFD}]"), "plane 16 private-use range is detected");
        ok(NgRuleSafety.containsPrivateUseRange("[\\uE000-\\uF8FF]"), "unicode escapes are detected");
        ok(!NgRuleSafety.containsPrivateUseRange("[\\x{1000}-\\x{2000}]"), "ordinary unicode range remains allowed");
        ok(NgRuleSafety.rejection("WORD", example, (short) NgRuleSafety.REGEX_FLAG) != null, "private-use regex is rejected");
        ok(NgRuleSafety.rejection("WORD", "短", (short) 0) == null, "one-character local word is accepted");
        ok(NgRuleSafety.rejection("WORD", "short", (short) 0) == null, "five-character local word is accepted");
        ok(NgRuleSafety.rejection("WORD", "abcdef", (short) 0) == null, "six-character word is accepted");
        ok(NgRuleSafety.rejection("WORD", "😀", (short) 0) == null, "one supplementary code point is accepted");
        ok(NgRuleSafety.rejection("WORD", " \t", (short) 0) != null, "blank local word is rejected");
        ok(NgRuleSafety.rejection("ID", "12345", (short) 0) == null, "short IDs are unaffected");
        ok(NgRuleSafety.rejection("NAME", "abc", (short) 0) == null, "short names are unaffected");
        ok(NgRuleSafety.rejection("ID", example, (short) NgRuleSafety.REGEX_FLAG) != null, "unsafe regex is rejected for IDs too");
        ok(NgRuleSafety.rejection("NAME", example, (short) NgRuleSafety.REGEX_FLAG) != null, "unsafe regex is rejected for names too");
        System.out.println("PASS " + checks + " NG rule safety checks");
    }
}
