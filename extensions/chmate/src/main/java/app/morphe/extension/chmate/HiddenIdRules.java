package app.morphe.extension.chmate;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/** Conservative ID suppression detection. Absence alone does not establish intent. */
public final class HiddenIdRules {
    public static final int HIDDEN_ID_FLAGS = 0x1008; // Native NGID + transparent NG.
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9+/]{8,12}");
    private static final Pattern NO_ID = Pattern.compile("\\?{3}(?:[0OP])?");
    private static final Pattern DATE = Pattern.compile("^\\d{4}/\\d{2}/\\d{2}.*", Pattern.DOTALL);
    private HiddenIdRules() { }

    public static boolean fiveCh(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || uri.getUserInfo() != null
                    || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("5ch.io") || host.endsWith(".5ch.io")
                    || host.equals("5ch.net") || host.endsWith(".5ch.net");
        } catch (RuntimeException ignored) { return false; }
    }

    public static boolean regularId(String id) {
        return id != null && !"DELETED".equals(id) && !"CAP_USER".equals(id)
                && !"OVER1000".equals(id) && ID.matcher(id).matches();
    }

    public static boolean candidate(int number, String id, String name, String date) {
        return number > 0 && (id == null || id.trim().isEmpty() || NO_ID.matcher(id.trim()).matches())
                && date != null && DATE.matcher(date).matches()
                && (name == null || !name.contains("★"));
    }

    public static int merge(boolean enabled, boolean fiveCh, boolean threadHasIds, boolean candidate, int flags) {
        return enabled && fiveCh && threadHasIds && candidate ? flags | HIDDEN_ID_FLAGS : flags;
    }
}
