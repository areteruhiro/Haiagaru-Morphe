import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import java.util.zip.ZipFile;

/** Read-only identification of native gesture-driven toolbar classes. */
public final class InspectPatrolToolbar {
    public static void main(String[] args) throws Exception {
        try (var zip = new ZipFile(args[0])) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes;
                try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                    if (!"Landroid/view/ViewGroup;".equals(cls.getSuperclass())) continue;
                    boolean gesture = false, point = false;
                    for (var f : cls.getFields()) {
                        gesture |= f.getType().equals("Landroid/view/GestureDetector;");
                        point |= f.getType().equals("Landroid/graphics/PointF;");
                    }
                    if (!gesture || !point) continue;
                    System.out.println("CLASS " + cls.getType());
                    for (var f : cls.getFields()) System.out.println(" FIELD " + f.getName() + " " + f.getType());
                    for (var m : cls.getMethods()) System.out.println(" METHOD " + m.getName() + m.getParameterTypes() + m.getReturnType());
                }
            }
        }
    }
}
