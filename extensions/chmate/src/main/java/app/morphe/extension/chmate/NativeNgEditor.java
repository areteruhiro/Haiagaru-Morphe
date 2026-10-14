package app.morphe.extension.chmate;

import android.app.Activity;
import android.content.Intent;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Open ChMate's NAME tab without adding or modifying any NG entries. */
public final class NativeNgEditor {
    private NativeNgEditor() {}

    static int nameOrdinal(Class<?> editor) throws ReflectiveOperationException {
        List<Class<?>> owners = new ArrayList<>();
        owners.add(editor);
        for (Class<?> nested : editor.getDeclaredClasses()) owners.add(nested);
        Integer ordinal = null;
        for (Class<?> owner : owners) for (Method method : owner.getDeclaredMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length != 3 || !"android.app.Activity".equals(parameters[0].getName())
                    || !parameters[1].isEnum()
                    || !"jp.syoboi.a2chMate.client.BBSUrlInfo".equals(parameters[2].getName())) continue;
            for (Object value : parameters[1].getEnumConstants()) {
                Enum<?> category = (Enum<?>) value;
                if (!"NAME".equals(category.name())) continue;
                if (ordinal != null && ordinal != category.ordinal())
                    throw new ReflectiveOperationException("Ambiguous native NGName tab");
                ordinal = category.ordinal();
            }
        }
        if (ordinal == null) throw new ReflectiveOperationException("Native NGName tab unavailable");
        return ordinal;
    }

    static void openName(Activity activity) throws ReflectiveOperationException {
        Class<?> editor = Class.forName("jp.syoboi.a2chMate.activity.NGEditorActivity", false,
                activity.getClassLoader());
        Intent intent = new Intent(activity, editor);
        intent.putExtra("ngType", nameOrdinal(editor));
        activity.startActivity(intent);
    }
}
