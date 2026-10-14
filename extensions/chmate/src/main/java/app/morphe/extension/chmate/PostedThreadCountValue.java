package app.morphe.extension.chmate;

/** Display-only limit; never used to trim postDataList or bookmarks. */
public final class PostedThreadCountValue {
    public static final int DEFAULT = 10;
    public static final int MAX = 10000;
    private PostedThreadCountValue() {}

    public static int parse(String raw) {
        try {
            int count = Integer.parseInt(raw.trim());
            return count >= 0 && count <= MAX ? count : -1;
        } catch (RuntimeException ignored) {
            return -1;
        }
    }

    public static int normalize(int count) {
        return count >= 0 && count <= MAX ? count : DEFAULT;
    }

    public static int defaultForVersion(String version) {
        return "0.8.10.242 dev".equals(version) || "0.8.10.243 dev".equals(version) ? DEFAULT : 0;
    }

    /** Positive SQL limit for legacy builders too; zero must not hide every row. */
    public static int queryLimit(int setting) {
        int value = normalize(setting);
        return value == 0 ? Integer.MAX_VALUE : value;
    }
}
