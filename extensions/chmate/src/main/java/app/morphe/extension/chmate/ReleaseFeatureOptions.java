package app.morphe.extension.chmate;

import java.util.ArrayList;
import java.util.List;

/** Stable release where an option was added or its existing feature was expanded.
 * Keep these versions fixed when publishing later releases. */
final class ReleaseFeatureOptions {
    enum Option {
        SETTINGS_LINKS("1.25.0"), SITE_GROUPS("1.25.0"),
        EDGE("1.25.0"), BAKUSAI("1.25.0"), FUTABA("1.25.0"), ANIMAN("1.25.0"),
        REPORTER_SEARCH("1.25.0"), REPORTER_MENU("1.25.0"), REPORTER_BOARD("1.25.0"),
        THREAD_THUMBNAILS("1.25.0"), VIDEO_THUMBNAILS("1.25.0"), VIDEO_IMMERSIVE("1.25.0"),
        IMAGE_NG("1.25.0"), COPY_PASTE_NG("1.25.0"), REPLACE_TXT("1.25.0"),
        HISTORY_TARGETS("1.25.0"), WATCH_AFTER_POSTING("1.25.0"), DELETE_READ("1.25.0"),
        IP_WIFI("1.25.0"), IP_MOBILE("1.25.0"), IMAGE_NG_SETTINGS("1.25.0"),
        ORG("1.26.0");

        final String version;
        Option(String version) { this.version = version; }
    }

    static String stableVersion(String version) {
        int suffix = version.indexOf('-');
        return suffix < 0 ? version : version.substring(0, suffix);
    }

    /** Prefer the stable accordion; before publication use only the newest dev. */
    static List<Option> forRelease(ReleaseHistoryText.Release release,
                                   List<ReleaseHistoryText.Release> visible) {
        String base = stableVersion(release.version);
        ReleaseHistoryText.Release owner = null;
        for (ReleaseHistoryText.Release candidate : visible) {
            if (!stableVersion(candidate.version).equals(base)) continue;
            if (owner == null) owner = candidate;
            if (!candidate.development) { owner = candidate; break; }
        }
        List<Option> result = new ArrayList<>();
        if (owner != release) return result;
        for (Option option : Option.values()) if (option.version.equals(base)) result.add(option);
        return result;
    }

    private ReleaseFeatureOptions() {}
}
