package app.morphe.extension.chmate;

import android.app.Activity;
import android.os.Build;
import android.view.View;

/** Back closes the viewer, including when MediaController owns the focused window. */
final class VideoViewerBack {
    private VideoViewerBack() { }

    static Runnable register(Activity activity) {
        return Build.VERSION.SDK_INT >= 33
                ? Api33.register(activity.getOnBackInvokedDispatcher(), activity::finish) : () -> { };
    }

    static Runnable register(View controller, Activity activity) {
        return Build.VERSION.SDK_INT >= 33
                ? Api33.register(controller.findOnBackInvokedDispatcher(), activity::finish) : () -> { };
    }

    private static final class Api33 {
        static Runnable register(android.window.OnBackInvokedDispatcher dispatcher, Runnable close) {
            if (dispatcher == null) return () -> { };
            android.window.OnBackInvokedCallback callback = close::run;
            dispatcher.registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_OVERLAY, callback);
            return () -> dispatcher.unregisterOnBackInvokedCallback(callback);
        }
    }
}
