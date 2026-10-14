import app.morphe.extension.chmate.NgNameConsolidation;

public final class VerifyNgNameConsolidation {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for (String name : new String[]{"(ﾜｯﾁｮｲ bbbb-cccc)", "(ｽｯﾌﾟ dddd-cccc)",
                "名無しさん (ﾜｯﾁｮｲW 1234-cccc)", "（ｽﾌﾟﾌﾟ Sd03-cccc）"})
            check("cccc".equals(NgNameConsolidation.suffix(name, (short) 0)), "carrier/full name");
        check("cC/+".equals(NgNameConsolidation.suffix("(ｽｯﾌﾟ Sd03-cC/+)", (short) 9)), "symbolic suffix");
        check(NgNameConsolidation.suffix("(ﾜｯﾁｮｲ bbbb-cccc).*", (short) 0) == null, "no trailing condition");
        for (short mode : new short[]{0x10, 0x20, 0x40})
            check(NgNameConsolidation.suffix("(ﾜｯﾁｮｲ bbbb-cccc)", mode) == null, "do not delete other match modes");
        check(!NgNameConsolidation.key("cccc", "poverty", (short) 0).equals(
                NgNameConsolidation.key("cccc", "edge", (short) 0)), "board groups separate");
        check(!NgNameConsolidation.key("cccc", null, (short) 0).equals(
                NgNameConsolidation.key("cccc", null, (short) 8)), "chain groups separate");
        check(!NgNameConsolidation.key("cccc", null, (short) 0).equals(
                NgNameConsolidation.key("cccc", null, (short) 1)), "hidden groups separate");
        check(!NgNameConsolidation.key("cccc", null, (short) 0).equals(
                NgNameConsolidation.key("cCcc", null, (short) 0)), "suffix case preserved");
        check(NgNameConsolidation.matches("(ﾜｯﾁｮｲ abcd-cCcc)", "edge", "CCCC", "edge"), "search insensitive");
        check(!NgNameConsolidation.matches("test", "edge", "", "poverty"), "scope exact");
        System.out.println("PASS NG search and safe consolidation planning");
    }
}
