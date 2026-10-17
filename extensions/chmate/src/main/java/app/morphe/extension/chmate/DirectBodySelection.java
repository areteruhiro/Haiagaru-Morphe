package app.morphe.extension.chmate;

import android.text.method.MovementMethod;
import android.view.MotionEvent;
import android.view.ActionMode;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

/** Opt-in framework selection on response bodies, never headers or editing fields. */
public final class DirectBodySelection {
    private static final WeakHashMap<TextView, State> STATES = new WeakHashMap<>();
    private DirectBodySelection() {}

    /** Preserve native listeners, but let TextView's own selection handle an opt-in long press. */
    public static View.OnLongClickListener longClickListener(Object object, View.OnLongClickListener nativeListener) {
        if (nativeListener == null || !isBody(object)) return nativeListener;
        return view -> Haiagaru.directBodySelectionEnabled() && isBody(view)
                ? false : nativeListener.onLongClick(view);
    }

    private static final class State {
        final boolean selectable, focusable, touchFocusable, clickable, longClickable;
        final MovementMethod movement;
        final ActionMode.Callback selectionCallback;
        final Method nativeDispatch;
        MotionEvent down;
        boolean moved, selectedAtDown;
        State(TextView view) throws ReflectiveOperationException {
            selectable = view.isTextSelectable();
            focusable = view.isFocusable();
            touchFocusable = view.isFocusableInTouchMode();
            clickable = view.isClickable();
            longClickable = view.isLongClickable();
            movement = view.getMovementMethod();
            selectionCallback = view.getCustomSelectionActionModeCallback();
            nativeDispatch = view.getClass().getMethod("haiagaruDirectSelectionNativeDispatch", MotionEvent.class);
        }
        void cancel() { if (down != null) { down.recycle(); down = null; } }
    }

    static boolean isBody(Object view) {
        if (!(view instanceof TextView)) return false;
        String name = view.getClass().getName();
        return name.equals("o.r8lambdaDC40CispcBM3LVwPP01tT_FjLc")
                || name.equals("o.setCornerTopRightRadius") || name.equals("o.zzbhp");
    }

    /** Called at binding, refresh and before touch, so recycled rows restore correctly. */
    public static boolean prepare(Object object) {
        if (!isBody(object)) return false;
        TextView view = (TextView) object;
        if (!Haiagaru.directBodySelectionEnabled()) {
            State previous = STATES.remove(view);
            if (previous != null) {
                previous.cancel();
                view.clearFocus();
                view.setTextIsSelectable(previous.selectable);
                view.setMovementMethod(previous.movement);
                view.setFocusable(previous.focusable);
                view.setFocusableInTouchMode(previous.touchFocusable);
                view.setClickable(previous.clickable);
                view.setLongClickable(previous.longClickable);
                view.setCustomSelectionActionModeCallback(previous.selectionCallback);
            }
            return false;
        }
        if (!STATES.containsKey(view)) {
            try { STATES.put(view, new State(view)); }
            catch (ReflectiveOperationException error) { return false; }
        }
        if (!view.isTextSelectable()) view.setTextIsSelectable(true);
        // ChMate 191's callback returns false from onCreateActionMode and posts
        // its native delayed deselection. Both prevent normal selection/Copy.
        // Use the platform callback while opted in, and restore ChMate's on opt-out.
        if (view.getCustomSelectionActionModeCallback() != null)
            view.setCustomSelectionActionModeCallback(null);
        return true;
    }

    /** Bypass native delayed selection/menu dispatch only for the opt-in body. */
    public static boolean dispatch(TextView view, MotionEvent event) {
        State state = STATES.get(view);
        if (state == null) return false;
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            state.cancel();
            state.down = MotionEvent.obtain(event);
            state.moved = false;
            state.selectedAtDown = view.hasSelection();
            // TextView selection must receive UP. Native row/list interception can
            // otherwise cancel the gesture after the long press, leaving only a magnifier.
            if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(true);
        }
        if (state.down != null) {
            int slop = ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
            if (Math.abs(event.getX() - state.down.getX()) > slop
                    || Math.abs(event.getY() - state.down.getY()) > slop
                    || event.getPointerCount() != 1) state.moved = true;
            // Ordinary vertical dragging still scrolls the thread. Keep interception
            // blocked only while starting/adjusting an actual text selection.
            if (state.moved && !state.selectedAtDown && !view.hasSelection()
                    && view.getParent() != null)
                view.getParent().requestDisallowInterceptTouchEvent(false);
            if (action == MotionEvent.ACTION_UP && !state.moved && !state.selectedAtDown
                    && !view.hasSelection()
                    && event.getEventTime() - state.down.getEventTime() < ViewConfiguration.getLongPressTimeout()) {
                // A short tap still uses ChMate's own URL/anchor/image dispatch.
                // Cancel framework handling before replay, avoiding duplicate URL launches.
                MotionEvent cancel = MotionEvent.obtain(event);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                view.onTouchEvent(cancel);
                cancel.recycle();
                try {
                    state.nativeDispatch.invoke(view, state.down);
                    state.nativeDispatch.invoke(view, event);
                } catch (ReflectiveOperationException error) {
                    android.util.Log.w("Haiagaru", "Cannot dispatch body tap", error);
                } finally { state.cancel(); }
                if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            }
        }
        // TextView handles word selection, draggable handles and the platform Copy menu.
        view.onTouchEvent(event);
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            state.cancel();
            if (view.getParent() != null) view.getParent().requestDisallowInterceptTouchEvent(false);
        }
        return true;
    }
}
