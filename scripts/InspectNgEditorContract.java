import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import java.util.zip.ZipFile;
/** Read-only native NG editor intent contract inspection. */
public final class InspectNgEditorContract {
    public static void main(String[] args) throws Exception {
        for (String apk : args) try (ZipFile zip = new ZipFile(apk)) {
            System.out.println(apk);
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var e = entries.nextElement(); if (!e.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var in = zip.getInputStream(e)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses())
                    for (var m : cls.getMethods()) {
                        if (m.getImplementation() == null) continue;
                        var code = new java.util.ArrayList<com.android.tools.smali.dexlib2.iface.instruction.Instruction>();
                        m.getImplementation().getInstructions().forEach(code::add);
                        if (code.stream().noneMatch(i -> i instanceof ReferenceInstruction r
                                && r.getReference() instanceof StringReference s && s.getString().equals("ngType"))) continue;
                        System.out.println(m);
                        for (var i : code) if (i instanceof ReferenceInstruction r) System.out.println(i.getOpcode() + " " + r.getReference());
                    }
            }
        }
    }
}
