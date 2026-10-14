package app.morphe.extension.chmate;

/** Structural screen containers can be empty while waiting for a Fragment. */
public final class AdContainerSafety {
    private AdContainerSafety() {}

    public static boolean isScreenHost(Class<?> type, int viewId) {
        if (viewId == 0x01020002) return true; // android.R.id.content
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if ("androidx.fragment.app.FragmentContainerView".equals(current.getName())) return true;
        }
        return false;
    }
}
