import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Field;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

public class VerifyThreadCommandBindings {
    static Map<String, ClassDef> classes;
    static Field field(String cls, String name) {
        for (ClassDef c = classes.get(cls); c != null; c = classes.get(c.getSuperclass()))
            for (Field f : c.getFields()) if (f.getName().equals(name)) return f;
        throw new AssertionError(cls + " missing " + name);
    }
    public static void main(String[] args) throws Exception {
        String[][] profiles = {
            {"Lo/p9ExternalSyntheticLambda6;", "d", "f", "y", "Lo/p9b;", "w", "o", "i"},
            {"Ljp/syoboi/a2chMate/feature/resedit/ResEditFragment;", "h", "a", "u", "Lo/accessgetBACKSLASHp;", "P", "r", "G"},
            {"Ljp/syoboi/a2chMate/feature/resedit/ResEditFragment;", "i", "b", "y", "Ljp/syoboi/a2chMate/feature/resedit/ResEditFragmentViewModel;", "T", "x", "A"}
        };
        for (int n = 0; n < args.length; n++) {
            classes = new HashMap<>();
            try (ZipFile zip = new ZipFile(args[n])) {
                for (var e : Collections.list(zip.entries())) {
                    if (!e.getName().matches("classes[0-9]*\\.dex")) continue;
                    try (var in = zip.getInputStream(e)) {
                        for (var c : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(in.readAllBytes())).getClasses()) classes.put(c.getType(), c);
                    }
                }
            }
            String[] p = profiles[n];
            String binding = field(p[0], p[1]).getType();
            String body = field(binding, p[2]).getType();
            boolean edit = false;
            for (String t = body; t != null;) {
                if (t.equals("Landroid/widget/EditText;")) { edit = true; break; }
                ClassDef c = classes.get(t); t = c == null ? null : c.getSuperclass();
            }
            if (!edit) throw new AssertionError("Not a native editor " + body);
            field(p[0], p[3]); field(p[4], p[6]);
            String target = field(p[4], p[5]).getType();
            boolean url = false, created = false;
            for (var m : classes.get(target).getMethods()) if (m.getName().equals(p[7]) && m.getParameterTypes().isEmpty() && m.getReturnType().equals("Ljava/lang/String;")) url = true;
            for (var m : classes.get(p[0]).getMethods()) if (m.getName().equals("onViewCreated")) created = true;
            if (!url || !created) throw new AssertionError("Native posting hook missing");
            System.out.println("PASS native posting adapter " + args[n]);
        }
    }
}
