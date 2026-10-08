package android.util;

/** Test-only Android logger stub: fail the test if the extension catches an error. */
public final class Log {
    public static int e(String tag, String message, Throwable error) {
        throw new AssertionError(message, error);
    }
}
