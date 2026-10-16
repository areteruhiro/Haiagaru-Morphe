package app.morphe.extension.chmate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;

/** A snapshot of exactly the native menu target. Never traverses loaded response lists. */
public final class ResponseArchiveEntry {
    public final String url, title, name, date, body;
    public final int number;

    public ResponseArchiveEntry(String url, String title, int number, String name, String date, String body) {
        URI uri = URI.create(url);
        if (number < 1 || uri.getHost() == null || uri.getUserInfo() != null
                || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || body == null || body.length() > 1000000)
            throw new IllegalArgumentException("Invalid response archive entry");
        this.url = url; this.title = title == null ? url : title;
        this.number = number; this.name = name == null ? "" : name;
        this.date = date == null ? "" : date; this.body = body;
    }

    public static ResponseArchiveEntry snapshot(Object response, Object board, String title)
            throws ReflectiveOperationException {
        String number, name, date, body, urlMethod;
        switch (response.getClass().getName()) {
            case "o.processAdDisplayErrorPostbackForUserError":
                number = "o"; name = "n"; date = "q"; body = "c"; urlMethod = "d"; break;
            case "o.BouncyCastleSocketAdapterCompanion":
                number = "l"; name = "m"; date = "t"; body = "d"; urlMethod = "e"; break;
            case "o.KeJ11":
                number = "o"; name = "n"; date = "p"; body = "h"; urlMethod = "c"; break;
            default: throw new IllegalArgumentException("Unsupported native response");
        }
        int no = ((Number) field(response, number)).intValue();
        Method method = board.getClass().getDeclaredMethod(urlMethod, int.class);
        method.setAccessible(true);
        return new ResponseArchiveEntry((String) method.invoke(board, no), title, no,
                (String) field(response, name), (String) field(response, date), (String) field(response, body));
    }

    static Object field(Object target, String name) throws ReflectiveOperationException {
        if (target == null) return null;
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try { Field f = type.getDeclaredField(name); f.setAccessible(true); return f.get(target); }
            catch (NoSuchFieldException ignored) { }
        }
        throw new NoSuchFieldException(name);
    }
}
