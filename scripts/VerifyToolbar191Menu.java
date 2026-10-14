import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.*;
import java.util.zip.ZipFile;

/** Execute the actual APK callback's boolean control flow, mocking dispatch results. */
public final class VerifyToolbar191Menu {
    public static void main(String[] args) throws Exception {
        Method callback = null;
        try (var zip = new ZipFile(args[0])) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] data;
                try (var in = zip.getInputStream(entry)) { data = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(data)).getClasses()) {
                    if (!cls.getType().equals("Lo/m6b$4;")) continue;
                    for (var method : cls.getMethods()) if (method.getName().equals("Xf_")) callback = method;
                }
            }
        }
        if (callback == null) throw new AssertionError("Missing callback");
        for (boolean fragment : new boolean[]{false, true}) {
            for (boolean options : new boolean[]{false, true}) verify(callback, fragment, options);
        }
        System.out.println("PASS: handled fragment stops dispatch; unhandled activity fallbacks preserved (4 cases)");
    }

    private static void verify(Method method, boolean fragment, boolean options) {
        List<Instruction> code = new ArrayList<>();
        Map<Integer, Integer> targets = new HashMap<>();
        List<Integer> offsets = new ArrayList<>();
        int address = 0;
        for (var ins : method.getImplementation().getInstructions()) {
            targets.put(address, code.size()); offsets.add(address); code.add(ins); address += ins.getCodeUnits();
        }
        int[] registers = new int[method.getImplementation().getRegisterCount()];
        int result = 0, fragmentCalls = 0, optionsCalls = 0, selectedCalls = 0;
        for (int pc = 0, steps = 0; steps < 100; steps++) {
            Instruction ins = code.get(pc);
            switch (ins.getOpcode()) {
                case IGET_OBJECT, CHECK_CAST, NOP -> {}
                case INSTANCE_OF -> registers[((TwoRegisterInstruction) ins).getRegisterA()] = 1;
                case CONST_4 -> registers[((OneRegisterInstruction) ins).getRegisterA()] = ((NarrowLiteralInstruction) ins).getNarrowLiteral();
                case MOVE_RESULT -> registers[((OneRegisterInstruction) ins).getRegisterA()] = result;
                case IF_EQZ, IF_NEZ -> {
                    boolean zero = registers[((OneRegisterInstruction) ins).getRegisterA()] == 0;
                    if (zero == (ins.getOpcode() == Opcode.IF_EQZ)) {
                        pc = targets.get(offsets.get(pc) + ((OffsetInstruction) ins).getCodeOffset()); continue;
                    }
                }
                case INVOKE_INTERFACE, INVOKE_VIRTUAL -> {
                    String name = ((MethodReference) ((ReferenceInstruction) ins).getReference()).getName();
                    switch (name) {
                        case "VQ_" -> { fragmentCalls++; result = fragment ? 1 : 0; }
                        case "onOptionsItemSelected" -> { optionsCalls++; result = options ? 1 : 0; }
                        case "onMenuItemSelected" -> { selectedCalls++; result = 1; }
                        default -> throw new AssertionError("Unexpected call " + name);
                    }
                }
                case RETURN -> {
                    if (registers[((OneRegisterInstruction) ins).getRegisterA()] != 1 || fragmentCalls != 1
                            || optionsCalls != (fragment ? 0 : 1)
                            || selectedCalls != (!fragment && !options ? 1 : 0))
                        throw new AssertionError("Dispatch regression fragment=" + fragment + " options=" + options
                                + " calls=" + fragmentCalls + "/" + optionsCalls + "/" + selectedCalls);
                    return;
                }
                default -> throw new AssertionError("Unsupported opcode " + ins.getOpcode());
            }
            pc++;
        }
        throw new AssertionError("Callback did not return");
    }
}
