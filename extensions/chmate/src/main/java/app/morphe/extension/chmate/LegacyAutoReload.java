package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.WeakHashMap;

/** 241/242 adapter: stock scrolling and stock network reload, without tailing mode. */
public final class LegacyAutoReload {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final WeakHashMap<Object, Session> SESSIONS = new WeakHashMap<>();
    private LegacyAutoReload() {}

    public static boolean supported(Context context) {
        try {
            String version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            return "0.8.10.241".equals(version) || "0.8.10.242 dev".equals(version);
        } catch (Exception ignored) { return false; }
    }

    public static int intervalSeconds() { return Haiagaru.autoReloadIntervalSeconds(); }

    /** Called only on the stock CLIENT_MIN_RELOAD_SEC branch; nonpositive values
     * are rejected by ChMate before this call. Do not weaken the board's limit. */
    public static int intervalForBoard(int seconds, int ignoredMin, int ignoredMax) {
        return Math.max(intervalSeconds(), Math.max(10, Math.min(60, seconds)));
    }

    public static boolean stopLongPress(Object fragment, View button) {
        if (button == null || button.getId() != 3 || !SESSIONS.containsKey(fragment)) return false;
        stop(fragment);
        Toast.makeText(button.getContext(), "自動リロードを停止しました", Toast.LENGTH_SHORT).show();
        return true;
    }

    /** The patched stock start site supplies its already validated minimum. */
    public static void start(Object fragment, long encodedMinimum) {
        Activity activity = null;
        try {
            activity = (Activity) fragment.getClass().getMethod("getActivity").invoke(fragment);
            if (activity == null) return;
            for (Object other : new ArrayList<>(SESSIONS.keySet())) stop(other);
            Session session = new Session(fragment, activity,
                    LegacyAutoReloadTiming.durationMillis(encodedMinimum));
            SESSIONS.put(fragment, session);
            session.startScrolling();
            MAIN.postDelayed(session, 250);
            Toast.makeText(activity, "自動リロードを開始しました", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            stop(fragment);
            Log.e("HaiagaruAutoReload", "Unable to start auto reload", error);
            if (activity != null) Toast.makeText(activity, "自動リロードを開始できませんでした", Toast.LENGTH_LONG).show();
        }
    }

    public static void stop(Object fragment) {
        Session session = SESSIONS.remove(fragment);
        if (session == null) return;
        MAIN.removeCallbacks(session);
        try {
            session.stopScroll.invoke(fragment);
        } catch (Exception error) {
            Log.w("HaiagaruAutoReload", "Unable to stop scrolling", error);
        }
        View view = session.list.get();
        if (view != null) view.setKeepScreenOn(false);
    }

    public static void activeTab(Object fragment, boolean active) {
        if (!active) stop(fragment);
    }

    public static void scrollState(Object fragment, boolean scrolling) {
        Session session = SESSIONS.get(fragment);
        if (session == null) return;
        session.scrolling = scrolling;
        View list = session.list.get();
        // A manual interruption above the bottom is not a reason to keep polling.
        if (!scrolling && list != null && list.canScrollVertically(1)
                && !session.timing.isLoading() && !session.starting) stop(fragment);
    }

    public static void event(Object fragment, Object event) {
        Session session = SESSIONS.get(fragment);
        if (session == null || event == null) return;
        String kind = event.getClass().getName();
        if (kind.equals(session.startEvent)) {
            session.timing.loading();
            session.loadingSince = SystemClock.elapsedRealtime();
        } else if (kind.equals(session.successEvent)) {
            session.timing.completed(true);
            session.settleUntil = SystemClock.elapsedRealtime() + 500;
        } else if (kind.equals(session.emptyEvent) || kind.equals(session.errorEvent)) {
            session.timing.completed(false);
            session.settleUntil = SystemClock.elapsedRealtime() + 500;
        } else if (kind.equals(session.finishedEvent) || kind.equals(session.invalidEvent)) {
            stop(fragment);
        }
    }

    private static Method method(Class<?> type, String name, Class<?>... arguments) throws Exception {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Method method = current.getDeclaredMethod(name, arguments);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) { }
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static final class Session implements Runnable {
        final WeakReference<Object> fragment;
        final WeakReference<View> list;
        final Method startScroll, stopScroll, reload, resumed, visible, busyValue, adapter, itemCount;
        final Object busyFlow;
        final WeakReference<Object> model;
        final LegacyAutoReloadTiming timing;
        final String startEvent, successEvent, emptyEvent, errorEvent, finishedEvent, invalidEvent;
        boolean scrolling, starting;
        long settleUntil, loadingSince;
        int beforeReloadCount;

        Session(Object target, Activity activity, long minimumMillis) throws Exception {
            boolean v241 = "0.8.10.241".equals(activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName);
            fragment = new WeakReference<>(target);
            View recycler = (View) field(target, v241 ? "m" : "n");
            if (recycler == null) throw new IllegalStateException("Response list is not ready");
            list = new WeakReference<>(recycler);
            Class<?> base = Class.forName("jp.syoboi.a2chMate.fragment.RoidonListFragment", false,
                    target.getClass().getClassLoader());
            startScroll = method(base, v241 ? "q" : "r");
            stopScroll = method(base, "p");
            Object lazy = field(target, v241 ? "s" : "t");
            Object nativeModel = method(lazy.getClass(), "getValue").invoke(lazy);
            model = new WeakReference<>(nativeModel);
            // i() only emits a navigation request. That request can be ignored,
            // leaving us waiting for a reload event that will never be emitted.
            reload = method(nativeModel.getClass(), "b", boolean.class);
            busyFlow = field(nativeModel, v241 ? "al" : "ai");
            busyValue = method(busyFlow.getClass(), v241 ? "b" : "d");
            adapter = method(recycler.getClass(), "getAdapter");
            Object nativeAdapter = adapter.invoke(recycler);
            if (nativeAdapter == null) throw new IllegalStateException("Response adapter is not ready");
            itemCount = method(nativeAdapter.getClass(), "getItemCount");
            resumed = target.getClass().getMethod("isResumed");
            visible = target.getClass().getMethod("isVisible");
            timing = new LegacyAutoReloadTiming(minimumMillis);
            String root = v241 ? "o.honorsDebugCertificates$" : "o.zzacz$";
            startEvent = root + (v241 ? "write" : "RemoteActionCompatParcelizer");
            successEvent = root + (v241 ? "_init_lambda2" : "r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ");
            emptyEvent = root + (v241 ? "IconCompatParcelizer" : "read");
            errorEvent = root + (v241 ? "ComponentActivity" : "write");
            finishedEvent = root + (v241 ? "read" : "ComponentActivity");
            invalidEvent = root + (v241 ? "RemoteActionCompatParcelizer" : "IconCompatParcelizer");
        }

        void startScrolling() throws Exception {
            Object target = fragment.get();
            View view = list.get();
            if (target == null || view == null) return;
            timing.reading();
            starting = true;
            try {
                if (view.canScrollVertically(1)) {
                    startScroll.invoke(target);
                    scrolling = true;
                }
                // Keep the display on while waiting too, not just while scrolling.
                view.setKeepScreenOn(true);
            } finally { starting = false; }
        }

        @Override public void run() {
            Object target = fragment.get();
            if (target == null || SESSIONS.get(target) != this) return;
            try {
                View view = list.get();
                if (view == null || !Boolean.TRUE.equals(resumed.invoke(target))
                        || !Boolean.TRUE.equals(visible.invoke(target)) || !view.hasWindowFocus()) {
                    stop(target);
                    return;
                }
                long now = SystemClock.elapsedRealtime();
                boolean nativeBusy = Boolean.TRUE.equals(busyValue.invoke(busyFlow));
                // Result flows can be missed while a collector changes lifecycle.
                // Native loading state is the final authority; never get stuck and
                // never issue a second request while its client is still running.
                if (timing.isLoading() && !nativeBusy && now - loadingSince >= 1000) {
                    Object currentAdapter = adapter.invoke(view);
                    int count = currentAdapter == null ? beforeReloadCount
                            : ((Number) itemCount.invoke(currentAdapter)).intValue();
                    timing.completed(count > beforeReloadCount);
                    settleUntil = now + 500;
                }
                if (!timing.isLoading() && !nativeBusy && now >= settleUntil) {
                    if (view.canScrollVertically(1)) {
                        timing.reading();
                        if (!scrolling) startScrolling();
                    } else {
                        if (scrolling) {
                            scrolling = false;
                            stopScroll.invoke(target);
                        }
                        view.setKeepScreenOn(true);
                        timing.bottom(now);
                        if (timing.due(now)) {
                            Object nativeModel = model.get();
                            if (nativeModel == null) { stop(target); return; }
                            Object currentAdapter = adapter.invoke(view);
                            beforeReloadCount = currentAdapter == null ? 0
                                    : ((Number) itemCount.invoke(currentAdapter)).intValue();
                            loadingSince = now;
                            timing.loading();
                            // Match the stock refresh button's force flag. false
                            // may only reuse the cached list after initial loading.
                            boolean accepted = Boolean.TRUE.equals(reload.invoke(nativeModel, true));
                            if (!accepted) {
                                timing.completed(false);
                                settleUntil = now + 500;
                            }
                        }
                    }
                }
                MAIN.postDelayed(this, 250);
            } catch (Exception error) {
                stop(target);
                Log.e("HaiagaruAutoReload", "Auto reload stopped after an error", error);
            }
        }
    }
}
