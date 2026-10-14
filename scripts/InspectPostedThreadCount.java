import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import java.util.zip.ZipFile;

/** Read-only inspection of posting-history preference keys in original APKs. */
public class InspectPostedThreadCount {
    public static void main(String[] args) throws Exception {
        for (String apk : args) {
            System.out.println(apk);
            try (ZipFile zip = new ZipFile(apk)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                    byte[] bytes;
                    try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                    for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                        for (var method : cls.getMethods()) {
                            if (method.getImplementation() == null) continue;
                            var instructions = new java.util.ArrayList<com.android.tools.smali.dexlib2.iface.instruction.Instruction>();
                            method.getImplementation().getInstructions().forEach(instructions::add);
                            for (int index = 0; index < instructions.size(); index++) {
                                var ins = instructions.get(index);
                                if (ins instanceof ReferenceInstruction ref && ref.getReference() instanceof StringReference str) {
                                    String s = str.getString();
                                    if ((cls.getType().contains("/prefs/Prefs;") && s.matches("(?i).*(count|recent|history|post).*")) || s.contains("書き込んだ") || s.contains("postDataListCount") || s.toLowerCase().contains("posted")) {
                                        System.out.println(cls.getType() + " " + method.getName() + " @" + index);
                                        for (int j = Math.max(0, index - 2); j < Math.min(instructions.size(), index + 6); j++) {
                                            var near = instructions.get(j);
                                            if (near instanceof ReferenceInstruction r) System.out.println(near.getOpcode() + " " + r.getReference());
                                            else if (near instanceof com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction n) System.out.println(near.getOpcode() + " " + n.getNarrowLiteral());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
