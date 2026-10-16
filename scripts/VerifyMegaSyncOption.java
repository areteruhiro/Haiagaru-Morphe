import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

/** Static regression checks; accepts alternating APK path and on/off expectation. */
public final class VerifyMegaSyncOption {
    private static final String SYNC = "Lapp/morphe/extension/chmate/HaiagaruMegaSync;";
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static boolean calls(Instruction instruction, String owner, String name) {
        return instruction instanceof ReferenceInstruction r
                && r.getReference() instanceof MethodReference m
                && m.getDefiningClass().equals(owner) && m.getName().equals(name);
    }
    private static int textIndex(List<Instruction> code, String value) {
        for (int i = 0; i < code.size(); i++)
            if (code.get(i) instanceof ReferenceInstruction r
                    && r.getReference() instanceof StringReference s && s.getString().equals(value)) return i;
        return -1;
    }
    public static void main(String[] args) throws Exception {
        require(args.length > 0 && args.length % 2 == 0, "APK on/off pairs required");
        for (int arg = 0; arg < args.length; arg += 2) try (var zip = new ZipFile(args[arg])) {
            var marker = zip.getEntry("assets/haiagaru/mega-sync/installed");
            boolean selected = args[arg + 1].equals("on");
            require((marker != null) == selected, "Patch selection mismatch");
            if (marker != null) try (var in = zip.getInputStream(marker)) {
                require(in.read() == '1', "Invalid marker");
            }
            Set<String> guarded = new HashSet<>();
            boolean sdkGuard = false, localActivity = false, localUi = false;
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses()) {
                    if (cls.getType().equals("Lapp/morphe/extension/chmate/HaiagaruLocalBackupActivity;")) localActivity = true;
                    if (!cls.getType().equals(SYNC)) continue;
                    for (Method method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        List<Instruction> code = new ArrayList<>();
                        method.getImplementation().getInstructions().forEach(code::add);
                        if (Set.of("maybeBackupOnStartup", "startSync", "showLoginDialog", "runConfiguredSync").contains(method.getName())) {
                            int firstInvoke = -1;
                            for (int i = 0; i < code.size(); i++)
                                if (code.get(i).getOpcode().name().startsWith("INVOKE_")) { firstInvoke = i; break; }
                            require(firstInvoke >= 0 && calls(code.get(firstInvoke), SYNC, "enabled"),
                                    "Cloud entry missing initial gate: " + method.getName());
                            require(code.get(firstInvoke + 2).getOpcode().name().startsWith("IF_"), "Missing gate branch");
                            guarded.add(method.getName());
                        }
                        if (method.getName().equals("enabled")) {
                            boolean sdk = false, api24 = false, installed = false;
                            for (Instruction i : code) {
                                if (i instanceof ReferenceInstruction r && r.getReference() instanceof FieldReference f)
                                    sdk |= f.getDefiningClass().equals("Landroid/os/Build$VERSION;") && f.getName().equals("SDK_INT");
                                if (i instanceof NarrowLiteralInstruction n) api24 |= n.getNarrowLiteral() == 24;
                                installed |= calls(i, SYNC, "installed");
                            }
                            sdkGuard = sdk && api24 && installed;
                        }
                        if (method.getName().equals("showDialog")) {
                            int cloud = textIndex(code, "MEGAの最新バックアップを確認して復元");
                            int local = textIndex(code, "端末のファイルにバックアップ");
                            int restore = textIndex(code, "端末のバックアップファイルから復元");
                            int[] addresses = new int[code.size()];
                            for (int i = 1; i < code.size(); i++) addresses[i] = addresses[i - 1] + code.get(i - 1).getCodeUnits();
                            int enabledRegister = -1;
                            for (int i = 0; i < code.size() - 1; i++)
                                if (calls(code.get(i), SYNC, "enabled")) enabledRegister = ((OneRegisterInstruction)code.get(i + 1)).getRegisterA();
                            boolean skipsCloud = false;
                            for (int i = 0; i < code.size(); i++) {
                                Instruction instruction = code.get(i);
                                if (instruction.getOpcode() == Opcode.IF_EQZ && instruction instanceof OneRegisterInstruction reg
                                        && reg.getRegisterA() == enabledRegister && instruction instanceof OffsetInstruction branch) {
                                    int target = addresses[i] + branch.getCodeOffset();
                                    skipsCloud |= cloud >= 0 && local > cloud && i < cloud
                                            && target > addresses[cloud] && target <= addresses[local];
                                }
                            }
                            localUi = skipsCloud && restore > local;
                        }
                    }
                }
            }
            require(guarded.size() == 4 && sdkGuard && localActivity && localUi, "Missing gate or local backup path");
            System.out.println("PASS " + args[arg] + " MEGA=" + selected + " API24/entry guards; local backup retained");
        }
    }
}
