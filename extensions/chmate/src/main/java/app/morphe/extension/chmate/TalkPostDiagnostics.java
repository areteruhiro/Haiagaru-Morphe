package app.morphe.extension.chmate;

import android.util.Log;

/** Stack-only diagnostics for caught Talk posting failures; never log request data. */
public final class TalkPostDiagnostics {
    private TalkPostDiagnostics() {}

    public static void record(Throwable error) {
        try {
            Throwable current = error;
            for (int depth = 0; current != null && depth < 8; depth++) {
                if (current instanceof NullPointerException) {
                    StringBuilder stack = new StringBuilder("Talk posting NullPointerException");
                    for (StackTraceElement frame : current.getStackTrace()) {
                        stack.append("\n at ").append(frame);
                    }
                    Log.e("HaiagaruTalk242", stack.toString());
                    return;
                }
                Throwable cause = current.getCause();
                if (cause == current) return;
                current = cause;
            }
        } catch (Throwable ignored) {
            // Diagnostics must not change the original posting failure or retry behavior.
        }
    }
}
