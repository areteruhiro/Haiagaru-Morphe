import app.morphe.extension.chmate.PostedThreadCountValue;

public final class VerifyPostedThreadCount {
    private static void check(boolean value) {
        if (!value) throw new AssertionError();
    }
    public static void main(String[] args) {
        check(PostedThreadCountValue.parse(" 25 ") == 25);
        check(PostedThreadCountValue.parse("1") == 1);
        check(PostedThreadCountValue.parse("10000") == 10000);
        check(PostedThreadCountValue.parse("0") == 0);
        for (String invalid : new String[]{"-1", "10001", "2147483648", "1.5", "", "abc"})
            check(PostedThreadCountValue.parse(invalid) == -1);
        check(PostedThreadCountValue.parse(null) == -1);
        check(PostedThreadCountValue.normalize(0) == 0);
        check(PostedThreadCountValue.normalize(10001) == 10);
        check(PostedThreadCountValue.normalize(150) == 150);
        check(PostedThreadCountValue.queryLimit(0) == Integer.MAX_VALUE);
        check(PostedThreadCountValue.queryLimit(25) == 25);
        for (String version : new String[]{"0.8.10.191 dev", "0.8.10.226 dev", "0.8.10.241"})
            check(PostedThreadCountValue.defaultForVersion(version) == 0);
        check(PostedThreadCountValue.defaultForVersion("0.8.10.242 dev") == 10);
        System.out.println("Posted-thread count validation passed");
    }
}
