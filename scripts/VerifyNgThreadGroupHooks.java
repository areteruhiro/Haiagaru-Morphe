import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

public final class VerifyNgThreadGroupHooks {
    public static void main(String[] args) throws Exception {
        String owner = switch (args[1]) {
            case "191" -> "Lo/m8a;";
            case "226" -> "Lo/getWriteBytesTotal$RemoteActionCompatParcelizer;";
            case "241" -> "Ljp/syoboi/a2chMate/ui/threadlist/ThreadListViewModel$RemoteActionCompatParcelizer;";
            case "242" -> "Ljp/syoboi/a2chMate/ui/threadlist/ThreadListViewModel$ComponentActivity;";
            default -> throw new IllegalArgumentException("version");
        };
        int count = 0;
        try (var zip = new ZipFile(args[0])) {
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes;
                try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses()) {
                    if (!cls.getType().equals(owner)) continue;
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        var ins = new ArrayList<Instruction>();
                        method.getImplementation().getInstructions().forEach(ins::add);
                        for (int i = 0; i < ins.size(); i++) {
                            if (!(ins.get(i) instanceof ReferenceInstruction ref)
                                || !(ref.getReference() instanceof MethodReference call)
                                || !call.getDefiningClass().equals("Lapp/morphe/extension/chmate/Haiagaru;")
                                || !call.getName().equals("ngThreadsForDisplay")) continue;
                            int words = method.getParameterTypes().size(); // all arguments are single-word here
                            int ngRegister = method.getImplementation().getRegisterCount() - words + 1;
                            if (i != 0 || !(ins.get(i) instanceof RegisterRangeInstruction range)
                                || range.getStartRegister() != ngRegister || range.getRegisterCount() != 1)
                                throw new AssertionError("Hook must read only p1 at method entry");
                            if (ins.get(i + 1).getOpcode() != Opcode.MOVE_RESULT_OBJECT
                                || ((OneRegisterInstruction) ins.get(i + 1)).getRegisterA() != ngRegister)
                                throw new AssertionError("Hook must replace only local p1");
                            if (args[1].equals("191") && ins.get(i + 2).getOpcode() != Opcode.CHECK_CAST)
                                throw new AssertionError("Legacy ArrayList cast missing");
                            count++;
                        }
                    }
                }
            }
        }
        if (count != 1) throw new AssertionError("Expected exactly one NG bucket hook: " + count);
        System.out.println("PASS NG bucket APK hook: " + args[1]);
    }
}
