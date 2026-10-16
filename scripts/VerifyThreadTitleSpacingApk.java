import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

public final class VerifyThreadTitleSpacingApk {
    public static void main(String[] args) throws Exception {
        for (String apk : args) try (var zip = new ZipFile(apk)) {
            int bindings = 0; boolean span = false, cleanup = false;
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses()) {
                    if (cls.getType().equals("Lapp/morphe/extension/chmate/ThreadTitleSpacing$Spacing;")) {
                        span = cls.getInterfaces().contains("Landroid/text/style/LineHeightSpan;");
                        cleanup = java.util.stream.StreamSupport.stream(cls.getFields().spliterator(), false)
                                .anyMatch(f -> f.getName().equals("adjustedDescent"));
                    }
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        for (var instruction : method.getImplementation().getInstructions()) {
                            if (!(instruction instanceof ReferenceInstruction r) || !(r.getReference() instanceof MethodReference m)) continue;
                            if (m.getDefiningClass().equals("Lapp/morphe/extension/chmate/ThreadTitleSpacing;") && m.getName().equals("apply")) {
                                if (!Set.of("getView", "onBindViewHolder").contains(method.getName()))
                                    throw new AssertionError("Unexpected title binding: " + method);
                                bindings++;
                            }
                            if (cls.getType().equals("Lapp/morphe/extension/chmate/ThreadTitleSpacing;")
                                    && Set.of("setIncludeFontPadding", "setPadding", "setLineSpacing", "setHeight").contains(m.getName()))
                                throw new AssertionError("Outer metrics still modified: " + m);
                        }
                    }
                }
            }
            if (bindings != 1 || !span || !cleanup) throw new AssertionError("Missing title hook/span/metric correction");
            System.out.println("PASS title-only binding and spacing span: " + apk);
        }
    }
}
