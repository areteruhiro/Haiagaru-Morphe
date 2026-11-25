package app.morphe.extension.chmate;

import android.net.Uri;
import java.lang.reflect.Method;

/** Do not replace a cleared Edge reply with a blank row in the native draft store. */
public final class EmptyPostDrafts {
    public static boolean handle(Object store, Object data) {
        if (store == null || data == null) return false;
        try {
            String type=data.getClass().getName();
            boolean old=type.equals("o.n7"), middle=type.equals("o.setBorderWidth");
            if (!old && !middle && !type.equals("jp.syoboi.a2chMate.postdata.PostData")) return false;
            String url=(String)NativeFields.find(data.getClass(),old?"l":"n").get(data);
            String body=(String)NativeFields.find(data.getClass(),old?"b":middle?"h":"g").get(data);
            long posted=NativeFields.find(data.getClass(),middle?"g":"j").getLong(data);
            if (posted != 0 || body == null || !body.isEmpty() || url == null) return false;
            String host=Uri.parse(url).getHost();
            if (!("bbs.eddibb.cc".equalsIgnoreCase(host) || "bbs.eddibb.net".equalsIgnoreCase(host)
                    || "bbs.eddibb.org".equalsIgnoreCase(host)) || !BoardUrlPicker.threadUrl(url)) return false;
            // Native lookup returns only the unsent draft for this exact thread;
            // native removal persists and notifies its observers. Never edit JSON directly.
            Method lookup=store.getClass().getMethod(old?"d":middle?"a":"c",String.class);
            Object previous=lookup.invoke(store,url);
            if (previous != null) {
                Method remove=store.getClass().getMethod(old?"c":"e",lookup.getReturnType());
                remove.invoke(store,previous);
            }
            return true;
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            android.util.Log.w("Haiagaru.Posting","Empty Edge draft cleanup unavailable",unavailable);
            return false;
        }
    }
    private EmptyPostDrafts() { }
}
