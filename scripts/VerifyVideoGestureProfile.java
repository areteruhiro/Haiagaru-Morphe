import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

public final class VerifyVideoGestureProfile {
    public static void main(String[] args) throws Exception {
        for (String apk : args) try (var zip = new ZipFile(apk)) {
            var classes = new ArrayList<ClassDef>();
            for (var e : Collections.list(zip.entries())) {
                if (!e.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var in = zip.getInputStream(e)) { bytes = in.readAllBytes(); }
                new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses().forEach(classes::add);
            }
            var layouts = classes.stream().filter(c -> "Landroid/widget/LinearLayout;".equals(c.getSuperclass())
                    && iterable(c.getMethods()).stream().anyMatch(m -> m.getName().equals("setOnGestureListener"))).toList();
            if (layouts.size() != 1) throw new AssertionError("Gesture layout ambiguity");
            var types = new HashSet<String>(); layouts.get(0).getFields().forEach(f -> types.add(f.getType()));
            var recognizers = classes.stream().filter(c -> types.contains(c.getType())
                    && iterable(c.getMethods()).stream().anyMatch(m -> m.getParameterTypes().equals(List.of("Landroid/view/MotionEvent;")) && m.getReturnType().equals("Z"))
                    && iterable(c.getMethods()).stream().anyMatch(m -> m.getName().equals("<init>") && m.getParameterTypes().size()==2 && m.getParameterTypes().get(0).equals("Landroid/content/Context;"))).toList();
            if (recognizers.size()!=1) throw new AssertionError("Recognizer ambiguity");
            var methods = iterable(recognizers.get(0).getMethods());
            long distances = methods.stream().filter(m -> m.getParameterTypes().equals(List.of("I")) && m.getReturnType().equals("V") && (m.getAccessFlags()&1)!=0).count();
            if (distances!=1) throw new AssertionError("Distance method ambiguity");
            System.out.println("PASS " + apk + " native=" + recognizers.get(0).getType());
        }
    }
    static <T> List<T> iterable(Iterable<T> values) { var result = new ArrayList<T>();values.forEach(result::add);return result; }
}
