package app.morphe.extension.chmate;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.ListView;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

/** Retain the current viewport across a display-only asynchronous preview resize. */
final class XPostPreviewPosition {
    private static final WeakHashMap<View, Anchor> PENDING = new WeakHashMap<>();

    static void beforeChange(View body) {
        for (ViewParent parent = body.getParent(); parent instanceof View; parent = parent.getParent()) {
            View view = (View) parent;
            boolean recycler = false;
            for (Class<?> type = view.getClass(); type != null; type = type.getSuperclass())
                if (type.getName().equals("androidx.recyclerview.widget.RecyclerView")) recycler = true;
            if (!(view instanceof ListView) && !recycler) continue;
            if (PENDING.containsKey(view) || !view.isShown()) return;
            try {
                Anchor anchor = new Anchor((ViewGroup) view);
                if (anchor.position < 0) return;
                PENDING.put(view, anchor);
                view.getViewTreeObserver().addOnPreDrawListener(anchor);
                view.postDelayed(anchor::close, 500);
            } catch (Exception ignored) { /* Unsupported list: leave native scrolling untouched. */ }
            return;
        }
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static final class Anchor implements ViewTreeObserver.OnPreDrawListener {
        final ViewGroup list;
        final Object adapter;
        final int count, position, top;
        final boolean bottom;
        final View lastRow;
        boolean closed;
        Anchor(ViewGroup list) throws Exception {
            this.list = list;
            adapter = call(list, "getAdapter", new Class<?>[0]);
            count = count();
            bottom = !list.canScrollVertically(1);
            lastRow = list.getChildCount() == 0 ? null : list.getChildAt(list.getChildCount() - 1);
            View first = list.getChildCount() == 0 ? null : list.getChildAt(0);
            position = first == null ? -1 : list instanceof ListView
                    ? ((ListView) list).getFirstVisiblePosition()
                    : (Integer) call(list, "getChildAdapterPosition", new Class<?>[]{View.class}, first);
            top = first == null ? 0 : first.getTop() - list.getPaddingTop();
        }
        int count() throws Exception {
            return (Integer) call(adapter, list instanceof ListView ? "getCount" : "getItemCount", new Class<?>[0]);
        }
        @Override public boolean onPreDraw() {
            if (closed) return true;
            if (list.isLayoutRequested()) return true;
            close();
            try {
                if (!list.isShown() || adapter != call(list, "getAdapter", new Class<?>[0]) || count != count()) return true;
                int target = position, offset = top;
                if (bottom && count > 0) {
                    View last = lastRow;
                    if (last == null) return true;
                    int lastPosition = list instanceof ListView ? ((ListView) list).getPositionForView(last)
                            : (Integer) call(list, "getChildAdapterPosition", new Class<?>[]{View.class}, last);
                    if (lastPosition != count - 1) return true;
                    target = count - 1;
                    offset = list.getHeight() - list.getPaddingBottom() - list.getPaddingTop() - last.getHeight();
                }
                if (list instanceof ListView) ((ListView) list).setSelectionFromTop(target, offset);
                else {
                    Object manager = call(list, "getLayoutManager", new Class<?>[0]);
                    call(list, "scrollToPosition", new Class<?>[]{int.class}, target);
                    // 242's inlined LinearLayoutManager pending offset, same binding as FilterPosition.
                    java.lang.reflect.Field field = manager.getClass().getDeclaredField("k");
                    field.setAccessible(true);
                    field.setInt(manager, offset);
                    list.requestLayout();
                }
                return false;
            } catch (Exception ignored) { return true; }
        }
        void close() {
            if (closed) return;
            closed = true;
            if (list.getViewTreeObserver().isAlive()) list.getViewTreeObserver().removeOnPreDrawListener(this);
            if (PENDING.get(list) == this) PENDING.remove(list);
        }
    }
}
