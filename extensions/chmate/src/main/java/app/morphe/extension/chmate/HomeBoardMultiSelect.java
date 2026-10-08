package app.morphe.extension.chmate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import android.util.Log;

/** Selects the threads belonging to a board header in the modern home list. */
public final class HomeBoardMultiSelect {
    private HomeBoardMultiSelect() {}

    public static boolean selectBoard(Object fragment, Object header) {
        try {
            Object boardId = boardIdOf(header);
            if (fragment == null || header == null || boardId == null || !isBoardHeader(header)) return false;

            Object viewModel = findHomeViewModel(fragment);
            if (viewModel == null) return false;

            List<?> items = findDisplayedItems(viewModel, header);
            int headerIndex = indexOf(items, header);
            if (items == null || headerIndex < 0) return false;

            Object selection = namedFieldValue(viewModel, "p");
            if (selection == null) return false;
            boolean version241 = header.getClass().getName().equals("o.MediationBannerAdapter$read");
            // Use exactly the mode and selection state read by the original row handler.
            if (!Boolean.TRUE.equals(flowValue(namedFieldValue(viewModel, version241 ? "f" : "i")))) return false;
            Object selectedValue = flowValue(namedFieldValue(selection, version241 ? "b" : "c"));
            if (!(selectedValue instanceof Set)) return false;
            Set<?> selected = new HashSet<>((Set<?>) selectedValue);

            Set<Long> threadIds = new LinkedHashSet<>();
            // Sorting can interleave board headers and threads from other boards.
            // Match the legacy 191 behavior: select every currently displayed
            // thread whose board ID matches this header, regardless of position.
            for (Object item : items) {
                if (item == null || isBoardHeader(item)) continue;
                Object itemBoard = boardIdOf(item);
                if (itemBoard == null || !sameBoard(boardId, itemBoard)) continue;
                Long id = threadIdOf(item);
                if (id != null && id.longValue() > 0L && id.longValue() != Long.MAX_VALUE) threadIds.add(id);
            }
            if (threadIds.isEmpty()) return false;

            boolean allSelected = true;
            for (Long id : threadIds) {
                if (!selected.contains(id)) {
                    allSelected = false;
                    break;
                }
            }
            boolean shouldSelect = !allSelected;
            Method toggle = selection.getClass().getDeclaredMethod(version241 ? "d" : "b", long.class);
            toggle.setAccessible(true);
            for (Long id : threadIds) {
                if (selected.contains(id) != shouldSelect) toggle.invoke(selection, id.longValue());
            }
            return true;
        } catch (Throwable error) {
            Log.e("Haiagaru", "Board-group selection could not be applied", error);
            return false;
        }
    }

    private static Object findHomeViewModel(Object fragment) throws Exception {
        for (Field field : fieldsOf(fragment.getClass())) {
            Object value = fieldValue(field, fragment);
            if (value == null) continue;
            if (value.getClass().getName().contains("HomeViewModel")) return value;
            try {
                Method getValue = value.getClass().getMethod("getValue");
                Object nested = getValue.invoke(value);
                if (nested != null && nested.getClass().getName().contains("HomeViewModel")) return nested;
            } catch (Throwable ignored) {
                // Not a lazy delegate.
            }
        }
        return null;
    }

    private static List<?> findDisplayedItems(Object viewModel, Object header) {
        for (Field field : fieldsOf(viewModel.getClass())) {
            Object state = flowValue(fieldValue(field, viewModel));
            if (!(state instanceof List)) continue;
            List<?> items = (List<?>) state;
            if (indexOf(items, header) >= 0) return items;
        }
        return null;
    }

    private static int indexOf(List<?> items, Object target) {
        if (items == null) return -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) == target || target.equals(items.get(i))) return i;
        }
        return -1;
    }

    private static Object boardIdOf(Object value) {
        if (value == null) return null;
        for (Field field : fieldsOf(value.getClass())) {
            Object nested = fieldValue(field, value);
            if (nested != null && nested.getClass().getName().endsWith(".BoardID")) return nested;
        }
        return null;
    }

    private static boolean sameBoard(Object first, Object second) {
        return first == second || first.equals(second);
    }

    private static boolean isBoardHeader(Object value) {
        if (value == null) return false;
        String name = value.getClass().getName();
        return name.equals("o.MediationBannerAdapter$read")
                || name.equals("o.getAvailabilityStatus$ComponentActivity");
    }

    private static Long threadIdOf(Object value) {
        if (value == null) return null;
        String name = value.getClass().getName();
        String field;
        if (name.equals("o.MediationBannerAdapter$r8lambdavCwjfXDiSGcirCy4I008VOiJ_lw")) field = "b";
        else if (name.equals("o.getAvailabilityStatus$r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ")) field = "a";
        else return null;
        Object id = namedFieldValue(value, field);
        return id instanceof Long ? (Long) id : null;
    }

    private static Object namedFieldValue(Object owner, String name) {
        if (owner == null) return null;
        for (Field field : fieldsOf(owner.getClass())) {
            if (name.equals(field.getName())) return fieldValue(field, owner);
        }
        return null;
    }

    private static Object flowValue(Object value) {
        if (value == null) return null;
        for (Class<?> current = value.getClass(); current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getParameterTypes().length != 0 || method.getReturnType() == void.class
                        || !("b".equals(method.getName()) || "d".equals(method.getName())
                        || "getValue".equals(method.getName()))) continue;
                try {
                    method.setAccessible(true);
                    Object result = method.invoke(value);
                    if (result instanceof Boolean || result instanceof Set || result instanceof List) return result;
                } catch (Throwable ignored) {
                    // Try another getter; not every no-arg method is a flow value accessor.
                }
            }
        }
        return null;
    }

    private static Object fieldValue(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<Field> fieldsOf(Class<?> type) {
        ArrayList<Field> fields = new ArrayList<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field[] declared = current.getDeclaredFields();
                for (Field field : declared) {
                    if (!Modifier.isStatic(field.getModifiers())) fields.add(field);
                }
            } catch (Throwable ignored) {
                // Continue with the superclass fields.
            }
        }
        return fields;
    }
}
