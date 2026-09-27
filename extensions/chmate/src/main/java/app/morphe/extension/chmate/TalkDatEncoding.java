package app.morphe.extension.chmate;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

final class TalkDatEncoding {
    private static final Charset MS932 = Charset.forName("MS932");

    private TalkDatEncoding() {
    }

    static byte[] encode(String value) {
        CharsetEncoder encoder = MS932.newEncoder();
        if (encoder.canEncode(value)) return value.getBytes(MS932);

        StringBuilder escaped = new StringBuilder(value.length() + 32);
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            // A standalone variation selector is intentionally omitted for
            // legacy ChMate rendering, but it is part of the grapheme when it
            // participates in a zero-width-joiner sequence. Dropping it turns
            // 👁️‍🗨️ into separate glyphs (and can produce U+FFFD).
            if (codePoint == 0xFE0F && !belongsToJoinedEmoji(value, offset)) {
                offset += Character.charCount(codePoint);
                continue;
            }
            String character = new String(Character.toChars(codePoint));
            if (encoder.canEncode(character)) {
                escaped.append(character);
            } else {
                escaped.append("&#").append(codePoint).append(';');
            }
            offset += Character.charCount(codePoint);
        }
        return escaped.toString().getBytes(MS932);
    }

    private static boolean belongsToJoinedEmoji(String value, int offset) {
        int next = offset + Character.charCount(value.codePointAt(offset));
        if (next < value.length() && value.codePointAt(next) == 0x200D) return true;
        int cursor = offset;
        while (cursor > 0) {
            int previous = value.codePointBefore(cursor);
            cursor -= Character.charCount(previous);
            if (previous == 0x200D) return true;
            if (Character.isWhitespace(previous) || Character.isLetterOrDigit(previous)) break;
        }
        return false;
    }
}
