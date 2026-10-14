import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import java.util.zip.ZipFile;

/** Prints only static checker templates and poverty board conditions from original APKs. */
public class InspectHissiTemplates {
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
                            for (var ins : method.getImplementation().getInstructions()) {
                                if (!(ins instanceof ReferenceInstruction ref)
                                        || !(ref.getReference() instanceof StringReference str)) continue;
                                String value = str.getString();
                                if (value.contains("hissi.org") || value.contains("poverty"))
                                    System.out.println(cls.getType() + " " + method.getName() + ": " + value);
                            }
                        }
                    }
                }
            }
        }
    }
}
