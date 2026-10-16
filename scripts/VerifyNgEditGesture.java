import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.zip.ZipFile;

/** Verify the native gesture completion hook and real compiled preference XML. */
public final class VerifyNgEditGesture {
    public static void main(String[] args) throws Exception {
        for (String apk : args) try (var zip = new ZipFile(apk)) {
            int hooks = 0, previews = 0, settings = 0;
            boolean helper = false;
            for (var entry : Collections.list(zip.entries())) {
                byte[] bytes;
                try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                if (entry.getName().endsWith(".xml")) {
                    if (new String(bytes, StandardCharsets.UTF_8).contains("haiagaruGestureNgEdit")
                            || new String(bytes, StandardCharsets.UTF_16LE).contains("haiagaruGestureNgEdit")) settings++;
                }
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses()) {
                    if (cls.getType().equals("Lapp/morphe/extension/chmate/NgEditGesture;")) helper = true;
                    if (!"Landroid/widget/LinearLayout;".equals(cls.getSuperclass())) continue;
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        for (var instruction : method.getImplementation().getInstructions()) {
                            if (instruction instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m
                                    && m.getDefiningClass().equals("Lapp/morphe/extension/chmate/NgEditGesture;")
                                    && m.getName().equals("onGesture")) {
                                if (!method.getParameterTypes().equals(java.util.List.of("I", "Landroid/graphics/PointF;", "Landroid/graphics/PointF;")))
                                    throw new AssertionError("Wrong completion hook: " + method);
                                hooks++;
                            }
                            if (instruction instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m
                                    && m.getDefiningClass().equals("Lapp/morphe/extension/chmate/NgEditGesture;")
                                    && m.getName().equals("preview")) {
                                if (!method.getName().equals("dispatchDraw"))
                                    throw new AssertionError("Preview hook outside native drawing: " + method);
                                previews++;
                            }
                        }
                    }
                }
            }
            if (!helper || hooks != 1 || previews != 1 || settings != 1)
                throw new AssertionError(apk + " helper=" + helper + " hooks=" + hooks + " previews=" + previews + " settings=" + settings);
            System.out.println("PASS " + apk + " completionHooks=1 previewHooks=1 gesturePreference=1");
        }
    }
}
