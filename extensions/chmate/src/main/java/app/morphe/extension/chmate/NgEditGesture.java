package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

/** Adds an unassigned-by-default action to ChMate's own gesture registry. */
public final class NgEditGesture {
    public static final String KEY = "haiagaruGestureNgEdit";
    private static final int ACTION = 0x48690042;
    private NgEditGesture() {}

    public static boolean onGesture(View view, int gesture) {
        if (gesture == 0 || !view.isShown()) return false;
        android.content.SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(view.getContext());
        if (preferences.getInt(KEY, 0) != gesture) return false;
        // Preserve existing actions when an already-used gesture is assigned.
        for (java.util.Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (entry.getKey().startsWith("gesture") && !entry.getKey().equals("gestureLength")
                    && entry.getValue() instanceof Integer && ((Integer) entry.getValue()) == gesture)
                return false;
        }
        return handle(view, ACTION);
    }

    public static boolean handle(View view, int action) {
        if (action != ACTION) return false;
        Context context = view.getContext();
        while (context instanceof ContextWrapper && !(context instanceof Activity)) {
            Context next = ((ContextWrapper) context).getBaseContext();
            if (next == context) break;
            context = next;
        }
        try {
            Class<?> editor = Class.forName("jp.syoboi.a2chMate.activity.NGEditorActivity", false,
                    context.getClassLoader());
            Intent intent = new Intent(context, editor);
            if (context instanceof Activity) ((Activity) context).startActivityForResult(intent, 0);
            else context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ReflectiveOperationException | RuntimeException error) {
            Toast.makeText(view.getContext(), "NG編集を開けませんでした", Toast.LENGTH_SHORT).show();
        }
        return true;
    }
}
