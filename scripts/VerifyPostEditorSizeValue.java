import app.morphe.extension.chmate.PostEditorSizeValue;
public final class VerifyPostEditorSizeValue {
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError();
    }
    public static void main(String[] args) {
        check(PostEditorSizeValue.parse(" 18.5 ") == 18.5f);
        check(PostEditorSizeValue.parse("1") == 1);
        check(PostEditorSizeValue.parse("200") == 200);
        for (String invalid : new String[]{"", "abc", "NaN", "Infinity", "0", "-1", "200.1"})
            check(PostEditorSizeValue.parse(invalid) == -1);
        check(PostEditorSizeValue.choice(0) == 0);
        for (int size = 10; size <= 36; size++) check(PostEditorSizeValue.choice(size) == size - 9);
        check(PostEditorSizeValue.choice(18.5f) == 28);
        check(PostEditorSizeValue.choice(48) == 28);
        check(PostEditorSizeValue.choice(1) == 28);
        System.out.println("PASS custom posting text-size validation and selection");
    }
}
