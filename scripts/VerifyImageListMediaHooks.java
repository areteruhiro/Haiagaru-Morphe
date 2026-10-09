import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.zip.ZipFile;

/** Checks the thread gallery, not the independently patched response thumbnails. */
public class VerifyImageListMediaHooks {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Provide patched APKs");
        for (String apk : args) {
            int galleryCalls = 0;
            try (ZipFile zip = new ZipFile(apk)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                    byte[] bytes;
                    try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                    for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                        for (var method : cls.getMethods()) {
                            boolean modern = cls.getType().equals("Lo/getMediaContent;")
                                    || cls.getType().equals("Lo/getWrappedCursor;");
                            if ((!method.getName().equals("onCreate") && !modern) || method.getImplementation() == null) continue;
                            boolean extractionSeen = false, expansionSeen = false;
                            var instructions = new java.util.ArrayList<com.android.tools.smali.dexlib2.iface.instruction.Instruction>();
                            method.getImplementation().getInstructions().forEach(instructions::add);
                            for (int index = 0; index < instructions.size(); index++) {
                                var instruction = instructions.get(index);
                                if (!(instruction instanceof ReferenceInstruction ref)
                                        || !(ref.getReference() instanceof MethodReference target)) continue;
                                if (target.getReturnType().equals("[Ljava/lang/String;")
                                        && target.getParameterTypes().contains("Ljava/lang/String;")) extractionSeen = true;
                                if (target.getDefiningClass().equals("Lapp/morphe/extension/chmate/ImgurAlbumAttachments;")
                                        && target.getName().equals("complete")) {
                                    if (!extractionSeen) throw new AssertionError("Expansion precedes native extraction: " + apk);
                                    if (modern && (index + 2 >= instructions.size()
                                            || instructions.get(index + 1).getOpcode() != com.android.tools.smali.dexlib2.Opcode.MOVE_RESULT_OBJECT
                                            || instructions.get(index + 2).getOpcode() != com.android.tools.smali.dexlib2.Opcode.ARRAY_LENGTH)) {
                                        throw new AssertionError("Modern expansion is not at the common gallery loop: " + apk);
                                    }
                                    galleryCalls++;
                                    expansionSeen = true;
                                }
                            }
                            if (expansionSeen) System.out.println("Gallery media hook: " + cls.getType());
                        }
                    }
                }
            }
            if (galleryCalls != 1) throw new AssertionError(apk + ": gallery media hooks=" + galleryCalls);
            System.out.println("PASS: " + apk + " native gallery extraction expanded exactly once");
        }
    }
}
