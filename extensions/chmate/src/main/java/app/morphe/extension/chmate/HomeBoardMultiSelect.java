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
import android.widget.ListView;

/** Selects the threads belonging to a board header in the modern home list. */
public final class HomeBoardMultiSelect {
    private HomeBoardMultiSelect() {}

    public static boolean selectLegacyRange(ListView list, int position) {
        if (list == null || list.getChoiceMode() != ListView.CHOICE_MODE_MULTIPLE
                || position < 0 || position >= list.getCount()) return false;
        try {
            if (!isLegacyHeading(list.getItemAtPosition(position))) return false;
            int end = position + 1;
            boolean allSelected = true;
            while (end < list.getCount() && !isLegacyHeading(list.getItemAtPosition(end))) {
                if (!list.isItemChecked(end)) allSelected = false;
                end++;
            }
            list.setItemChecked(position, false);
            for (int i = position + 1; i < end; i++) list.setItemChecked(i, !allSelected);
            return true;
        } catch (Throwable error) {
            Log.e("Haiagaru", "Legacy heading selection could not be applied", error);
            return false;
        }
    }

    private static boolean isLegacyHeading(Object row) throws Exception {
        if (row == null || !row.getClass().getName().startsWith("o.Yy2$")) return false;
        Method type = row.getClass().getMethod("a");
        type.setAccessible(true);
        int kind = ((Number) type.invoke(row)).intValue();
        // Board, keyword/rating and search-result section headings.
        return kind == 0 || kind == 2 || kind == 3 || kind == 4;
    }

    public static boolean selectBoard(Object fragment, Object header) {
        try {
            if (fragment == null || header == null || !isBoardHeader(header)) return false;

            Object viewModel = findHomeViewModel(fragment);
            if (viewModel == null) return false;

            List<?> items = findDisplayedItems(viewModel, header);
            int headerIndex = indexOf(items, header);
            if (items == null || headerIndex < 0) return false;

            Object selection = namedFieldValue(viewModel, "p");
            if (selection == null) return false;
            boolean version241 = header.getClass().getName().startsWith("o.MediationBannerAdapter$");
            // Use exactly the mode and selection state read by the original row handler.
            if (!Boolean.TRUE.equals(flowValue(namedFieldValue(viewModel, version241 ? "f" : "i")))) return false;
            Object selectedValue = flowValue(namedFieldValue(selection, version241 ? "b" : "c"));
            if (!(selectedValue instanceof Set)) return false;
            Set<?> selected = new HashSet<>((Set<?>) selectedValue);

            Set<Long> threadIds = new LinkedHashSet<>();
            for (int i = headerIndex + 1; i < items.size(); i++) {
                Object item = items.get(i);
                if (item == null) break;
                if (isBoardHeader(item)) break;
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
                getValue.setAccessible(true);
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

    private static boolean isBoardHeader(Object value) {
        if (value == null) return false;
        String name = value.getClass().getName();
        String getter;
        if (name.startsWith("o.MediationBannerAdapter$")) getter = "e";
        else if (name.startsWith("o.getAvailabilityStatus$")) getter = "a";
        else return false;
        // Both models reserve MAX_VALUE for headings, including keyword and
        // rating groups which do not carry a BoardID. Thread bookmark IDs differ.
        try {
            Method method = value.getClass().getMethod(getter);
            if (method.getReturnType() != long.class) return false;
            method.setAccessible(true);
            return Long.valueOf(Long.MAX_VALUE).equals(method.invoke(value));
        } catch (Throwable ignored) {
            return false;
        }
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
