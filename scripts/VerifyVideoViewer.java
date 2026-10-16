import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

public final class VerifyVideoViewer {
    public static void main(String[] args) throws Exception {
        for (String apk : args) try (var zip = new ZipFile(apk)) {
            int hooks = 0, forResult = 0;
            boolean player = false, external = false, setting = false;
            boolean manualControls = false, transparentButtons = false;
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses()) {
                    boolean viewer = cls.getType().equals("Lapp/morphe/extension/chmate/VideoViewerActivity;");
                    boolean controller = cls.getType().startsWith("Lapp/morphe/extension/chmate/VideoViewerActivity$")
                            && "Landroid/widget/MediaController;".equals(cls.getSuperclass());
                    if (viewer) player = "Landroid/app/Activity;".equals(cls.getSuperclass());
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        var body = new ArrayList<com.android.tools.smali.dexlib2.iface.instruction.Instruction>();
                        method.getImplementation().getInstructions().forEach(body::add);
                        int[] addresses = new int[body.size()];
                        for(int n=1;n<body.size();n++) addresses[n]=addresses[n-1]+body.get(n-1).getCodeUnits();
                        for (int i = 0; i < body.size(); i++) {
                            if (!(body.get(i) instanceof ReferenceInstruction r)) continue;
                            if (r.getReference() instanceof StringReference s && s.getString().equals("videoViewerEnabled")
                                    && cls.getType().equals("Lapp/morphe/extension/chmate/Haiagaru;")) setting = true;
                            if (!(r.getReference() instanceof MethodReference m)) continue;
                            if (controller && method.getName().equals("show")
                                    && m.getDefiningClass().equals("Landroid/widget/MediaController;") && m.getName().equals("show")
                                    && i > 0 && body.get(i - 1) instanceof com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction literal
                                    && literal.getNarrowLiteral() == 0
                                    && body.get(i - 1) instanceof com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction constant
                                    && body.get(i) instanceof com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction invoke
                                    && constant.getRegisterA() == invoke.getRegisterD()) manualControls = true;
                            if (viewer && method.getName().equals("overlayButton") && m.getName().equals("setShadowLayer"))
                                transparentButtons = body.stream().anyMatch(ins -> ins instanceof ReferenceInstruction ref
                                        && ref.getReference() instanceof MethodReference called && called.getName().equals("setBackgroundColor"));
                            if (viewer && m.getDefiningClass().equals("Landroid/content/Intent;")
                                    && m.getName().equals("createChooser")) external = true;
                            if (m.getDefiningClass().equals("Lapp/morphe/extension/chmate/VideoViewerActivity;")
                                    && m.getName().equals("prepareExternalIntent")) {
                                if (cls.getType().startsWith("Lapp/morphe/extension/")) throw new AssertionError("External menu recursively intercepted");
                                if (!(body.get(i + 1) instanceof ReferenceInstruction next)
                                        || !(next.getReference() instanceof MethodReference launch)
                                        || !Set.of("startActivity", "startActivityForResult").contains(launch.getName()))
                                    throw new AssertionError("Not an activity launch boundary");
                                if (launch.getName().equals("startActivityForResult")) forResult++;
                                for (int n=0;n<body.size();n++) {
                                    if (body.get(n) instanceof com.android.tools.smali.dexlib2.iface.instruction.OffsetInstruction branch
                                            && addresses[n]+branch.getCodeOffset()==addresses[i+1])
                                        throw new AssertionError("Branch skips viewer hook: " + method);
                                }
                                hooks++;
                            }
                        }
                    }
                }
            }
            if (!player || !external || !setting || !manualControls || !transparentButtons || hooks == 0 || forResult == 0)
                throw new AssertionError("Incomplete viewer " + apk);
            System.out.println("PASS " + apk + " player/setting/external-menu launchHooks=" + hooks + " forResult=" + forResult);
        }
    }
}
