import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.util.*;
import java.util.zip.ZipFile;

public final class InspectHiddenNgMenu {
    public static void main(String[] args) throws Exception {
        long target = Long.decode(args[1]);
        try (var zip = new ZipFile(args[0])) {
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        var instructions = new ArrayList<Instruction>();
                        method.getImplementation().getInstructions().forEach(instructions::add);
                        boolean hit = instructions.stream().anyMatch(ins -> ins instanceof WideLiteralInstruction literal
                                && literal.getWideLiteral() == target);
                        if (!hit) continue;
                        System.out.println("METHOD " + method + " superclass=" + cls.getSuperclass());
                        for (int i = 0; i < instructions.size(); i++) {
                            var ins = instructions.get(i);
                            String detail = ins instanceof ReferenceInstruction r ? " " + r.getReference()
                                    : ins instanceof WideLiteralInstruction l ? " " + l.getWideLiteral() : "";
                            System.out.println(i + " " + ins.getOpcode() + detail);
                        }
                    }
                }
            }
        }
    }
}
