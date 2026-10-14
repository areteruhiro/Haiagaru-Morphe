package android.util;

/** JVM-only test stand-in, never included in the Android extension. */
public final class Base64 {
    public static final int DEFAULT = 0;
    public static byte[] decode(String value, int ignored) {
        return java.util.Base64.getMimeDecoder().decode(value);
    }
}
