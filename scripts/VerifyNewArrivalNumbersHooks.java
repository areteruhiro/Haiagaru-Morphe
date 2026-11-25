import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.io.BufferedInputStream;
import java.util.*;
import java.util.zip.ZipFile;

public final class VerifyNewArrivalNumbersHooks {
    public static void main(String[] arguments) throws Exception {
        Map<String,Integer> counts = new HashMap<>();
        try (ZipFile archive = new ZipFile(arguments[0])) {
            for (var entries = archive.entries(); entries.hasMoreElements();) {
                var entry = entries.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                try (var input = new BufferedInputStream(archive.getInputStream(entry))) {
                    for (var owner : DexBackedDexFile.fromInputStream(Opcodes.getDefault(), input).getClasses())
                        for (var method : owner.getMethods()) {
                            if (method.getImplementation() == null) continue;
                            List<Instruction> code = new ArrayList<>();
                            method.getImplementation().getInstructions().forEach(code::add);
                            for (int index = 0; index < code.size(); index++) {
                                if (!(code.get(index) instanceof ReferenceInstruction reference)
                                        || !(reference.getReference() instanceof MethodReference call)) continue;
                                if (call.getDefiningClass().contains("ZeroNewArrivalRetry")) throw new AssertionError("Retry hook remains");
                                if (owner.getType().startsWith("Lapp/morphe/extension/")) continue;
                                if (!call.getDefiningClass().equals("Lapp/morphe/extension/chmate/NewArrivalNumbers;")) continue;
                                counts.merge(call.getName(),1,Integer::sum);
                                if (!(code.get(index) instanceof FiveRegisterInstruction registers)
                                        || registers.getRegisterCount() != (call.getName().equals("count") ? 3 : 2))
                                    throw new AssertionError("Invalid number-count operands");
                                if (!call.getName().equals("after") && code.get(index+1).getOpcode() != com.android.tools.smali.dexlib2.Opcode.MOVE_RESULT)
                                    throw new AssertionError("Native size/count result is not retained");
                            }
                        }
                }
            }
        }
        if (!counts.equals(Map.of("before",1,"after",1,"count",1))) throw new AssertionError(counts);
        System.out.println("PASS old-number snapshot, current-number snapshot, numbered count; no retry hook");
    }
}
