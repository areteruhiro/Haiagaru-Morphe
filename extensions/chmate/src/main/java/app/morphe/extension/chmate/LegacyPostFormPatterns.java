package app.morphe.extension.chmate;

import java.util.regex.Pattern;

/** 191 shares this form parser between 5ch and Talk; values can span lines. */
public final class LegacyPostFormPatterns {
    private LegacyPostFormPatterns() {}

    public static Pattern compile(String expression) {
        return Pattern.compile(expression, Pattern.DOTALL);
    }
}
