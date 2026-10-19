import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

/** Compares final APKs with native inputs: the caller must keep all its registers and branches. */
public class VerifyResumeReloadHooks {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static ClassDef fragment(String path) throws Exception {
        try (var zip = new ZipFile(path)) {
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                for (var type : new DexBackedDexFile(null, ByteBuffer.wrap(zip.getInputStream(entry).readAllBytes())).getClasses())
                    if (Set.of("Lo/pa;", "Ljp/syoboi/a2chMate/ui/reslist/ResListFragment;").contains(type.getType())) return type;
            }
        }
        throw new AssertionError("Missing fragment: " + path);
    }
    static Method method(ClassDef owner, String name) {
        for (var method : owner.getMethods()) if (method.getName().equals(name)) return method;
        throw new AssertionError("Missing method: " + name);
    }
    static List<Instruction> code(Method method) {
        var result = new ArrayList<Instruction>(); method.getImplementation().getInstructions().forEach(result::add); return result;
    }
    static MethodReference reference(Instruction instruction) {
        return instruction instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m ? m : null;
    }
    public static void main(String[] paths) throws Exception {
        check(paths.length % 2 == 0, "Provide native/patched APK pairs");
        for (int pair = 0; pair < paths.length; pair += 2) {
            var nativeClass = fragment(paths[pair]); var patched = fragment(paths[pair + 1]);
            var original = method(nativeClass, "onResume"); var resume = method(patched, "onResume");
            check(original.getImplementation().getRegisterCount() == resume.getImplementation().getRegisterCount(), "Caller register count changed");
            var before = code(original); var after = code(resume);
            check(before.size() == after.size(), "Caller instruction count changed");
            int replaced = 0;
            for (int i = 0; i < before.size(); i++) {
                var left = before.get(i); var right = after.get(i); var ref = reference(right);
                if (ref != null && ref.getName().equals("haiagaruResumeReload")) {
                    replaced++;
                    check(right.getOpcode().name().equals("INVOKE_STATIC"), "Guard must be a static call");
                    check(reference(left).getParameterTypes().equals(List.of("I", "Ljava/lang/Object;")), "Wrong native reload was replaced");
                    var a = (FiveRegisterInstruction) left; var b = (FiveRegisterInstruction) right;
                    check(a.getRegisterCount() == b.getRegisterCount() && a.getRegisterC() == b.getRegisterC()
                            && a.getRegisterD() == b.getRegisterD() && a.getRegisterE() == b.getRegisterE(), "Argument registers changed");
                } else {
                    check(left.getOpcode() == right.getOpcode() && left.getCodeUnits() == right.getCodeUnits(), "Native instruction changed at " + i);
                    if (left instanceof OneRegisterInstruction a) check(a.getRegisterA() == ((OneRegisterInstruction) right).getRegisterA(), "Native destination changed");
                    if (left instanceof OffsetInstruction a) check(a.getCodeOffset() == ((OffsetInstruction) right).getCodeOffset(), "Native branch changed");
                    if (left instanceof ReferenceInstruction a) check(a.getReference().toString().equals(((ReferenceInstruction) right).getReference().toString()), "Native reference changed");
                }
            }
            check(replaced == 1, "Exactly one automatic reload must be guarded");
            var wrapper = method(patched, "haiagaruResumeReload");
            check(wrapper.getImplementation().getRegisterCount() == 4, "Wrapper must own its scratch register");
            check(wrapper.getReturnType().equals("Z"), "Native Boolean result must be preserved");
            check(code(wrapper).stream().anyMatch(i -> reference(i) != null && reference(i).getName().equals("shouldSkipTabletResumeReload")), "Missing policy guard");
            System.out.println("PASS native caller registers, arguments, branches, initial load and guarded Boolean result: " + paths[pair + 1]);
        }
    }
}
