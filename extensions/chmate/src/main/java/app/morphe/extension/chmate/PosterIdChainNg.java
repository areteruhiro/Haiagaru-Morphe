package app.morphe.extension.chmate;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Extends native reply chains to the other posts of an author suppressed by the same WORD rule. */
public final class PosterIdChainNg {
    private static final int CHAIN = 8;
    private static final int ALSO_ID = 256;
    private static final Map<Object, Set<String>> AUTHORS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private PosterIdChainNg() { }

    /** Legacy adapters are reused when the thread or native NG registrations change. */
    public static void reset(Object owner) {
        AUTHORS.remove(owner);
    }

    /** Runs before the native chain-state write, so native ancestor traversal sees these roots too. */
    public static int merge(Object owner, Object response, int original) {
        if (original != 0 || owner == null || response == null) return original;
        try {
            Profile profile = profile(response.getClass().getSimpleName());
            Set<String> ids = AUTHORS.get(owner);
            if (ids == null) {
                ids = Collections.unmodifiableSet(authors(owner, response.getClass(), profile));
                AUTHORS.put(owner, ids);
            }
            Object id = field(response, profile.id);
            return id instanceof String && ids.contains(id) ? 1 : original;
        } catch (ReflectiveOperationException | RuntimeException error) {
            AUTHORS.put(owner, Collections.emptySet());
            Log.w("Haiagaru", "Poster ID chain NG: " + error.getClass().getSimpleName());
            return original;
        }
    }

    private static Set<String> authors(Object owner, Class<?> rowType, Profile profile)
            throws ReflectiveOperationException {
        Object matcher = field(owner, profile.word);
        List<Pattern> patterns = patterns(matcher);
        Set<String> ids = new HashSet<>();
        if (patterns.isEmpty()) return ids;
        Object source = field(owner, profile.rows);
        if (!(source instanceof List)) return ids;
        try {
            Object base = field(source, "mBase");
            if (base instanceof List) source = base;
        } catch (NoSuchFieldException ignored) { }
        for (Object row : (List<?>) source) {
            if (!rowType.isInstance(row)) continue;
            Object rawId = field(row, profile.id);
            if (!(rawId instanceof String)) continue;
            String id = (String) rawId;
            if (!usableId(id) || ids.contains(id)) continue;
            Object rawBody = field(row, profile.body);
            if (!(rawBody instanceof String)) continue;
            for (Pattern pattern : patterns) {
                if (pattern.matcher((String) rawBody).find()) {
                    ids.add(id);
                    break;
                }
            }
        }
        return ids;
    }

    private static boolean usableId(String id) {
        return !id.isEmpty() && !id.startsWith("?") && !"DELETED".equals(id)
                && !"CAP_USER".equals(id) && !"OVER1000".equals(id);
    }

    /** Uses the board-filtered native WORD matcher, never another thread's board or NGID list. */
    private static List<Pattern> patterns(Object matcher) throws ReflectiveOperationException {
        List<Pattern> result = new ArrayList<>();
        if (matcher == null) return result;
        for (Class<?> type = matcher.getClass(); type != null; type = type.getSuperclass()) {
            for (Field candidate : type.getDeclaredFields()) {
                if (Modifier.isStatic(candidate.getModifiers()) || !List.class.isAssignableFrom(candidate.getType())) continue;
                candidate.setAccessible(true);
                Object value = candidate.get(matcher);
                if (!(value instanceof List)) continue;
                for (Object item : (List<?>) value) {
                    if (item == null || !item.getClass().getName().equals("jp.syoboi.a2chMate.ng.NGWord$Item")) continue;
                    int flags = flags(item);
                    if ((flags & (CHAIN | ALSO_ID)) != (CHAIN | ALSO_ID)
                            || (flags & (4 | 64 | 128)) != 0) continue;
                    String word = item.toString();
                    if (word.isEmpty()) continue;
                    int mode = flags & 0x70;
                    String expression;
                    if (mode == 0x20) expression = word;
                    else if (mode == 0 && word.startsWith("/")) {
                        expression = word.substring(1);
                        try { Pattern.compile(expression); }
                        catch (PatternSyntaxException ignored) { expression = Pattern.quote(word); }
                    } else expression = Pattern.quote(word);
                    try { result.add(Pattern.compile(expression, Pattern.CASE_INSENSITIVE)); }
                    catch (PatternSyntaxException ignored) { }
                }
            }
        }
        return result;
    }

    private static int flags(Object item) throws IllegalAccessException {
        for (Field candidate : item.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(candidate.getModifiers()) && candidate.getType() == short.class) {
                candidate.setAccessible(true);
                return candidate.getShort(item) & 0xffff;
            }
        }
        throw new IllegalArgumentException("Native NG item flags unavailable");
    }

    private static Object field(Object value, String name) throws ReflectiveOperationException {
        return ResponseArchiveEntry.field(value, name);
    }

    private static Profile profile(String row) {
        switch (row) {
            case "processAdDisplayErrorPostbackForUserError": return new Profile("K", "C", "h", "c");
            case "BouncyCastleSocketAdapterCompanion": return new Profile("J", "A", "j", "d");
            case "setDislikeWidth": return new Profile("e", "b", "i", "g");
            case "KeJ11": return new Profile("a", "c", "g", "h");
            case "zzabv": return new Profile("b", "a", "h", "j");
            default: throw new IllegalArgumentException("Unsupported native NG model: " + row);
        }
    }

    private static final class Profile {
        final String rows, word, id, body;
        Profile(String rows, String word, String id, String body) {
            this.rows = rows; this.word = word; this.id = id; this.body = body;
        }
    }
}
