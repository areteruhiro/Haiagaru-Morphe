import java.lang.reflect.*;
import java.util.*;

/** Exercises the actual compiled extension resolver; no Android objects or data. */
public class VerifyDeleteReadGesture {
    public static class Named {
        public List<?> getFragments() { return null; }
        public List<?> other() { return null; }
    }
    public static class Obfuscated {
        public List<?> q() { return null; }
        public List<?> withArgument(int id) { return null; }
        public static List<?> staticList() { return null; }
        private List<?> privateList() { return null; }
        public Object wrongType() { return null; }
    }
    public static class Ambiguous {
        public List<?> a() { return null; }
        public List<?> b() { return null; }
    }
    public static class Missing { public Object q() { return null; } }
    public static void main(String[] args) throws Exception {
        Class<?> type = Class.forName("app.morphe.extension.chmate.DeleteReadThreads");
        Method resolve = type.getDeclaredMethod("noArgMethod", Class.class, String.class, Class.class);
        resolve.setAccessible(true);
        Method named = (Method) resolve.invoke(null, Named.class, "getFragments", List.class);
        if (!named.getName().equals("getFragments")) throw new AssertionError("Named API must win");
        Method obfuscated = (Method) resolve.invoke(null, Obfuscated.class, "getFragments", List.class);
        if (!obfuscated.getName().equals("q")) throw new AssertionError("Exact signature only");
        for (Class<?> bad : new Class<?>[]{Ambiguous.class, Missing.class}) {
            try {
                resolve.invoke(null, bad, "getFragments", List.class);
                throw new AssertionError("Must fail closed: " + bad);
            } catch (InvocationTargetException e) {
                if (!(e.getCause() instanceof NoSuchMethodException)) throw e;
            }
        }
        System.out.println("PASS 4 compiled gesture API resolution checks");
    }
}
