import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.*;
import java.util.zip.ZipFile;

/** Final APK checks: posting-only menu hooks and no editor-wrapper button hook. */
public final class VerifyImgurPostingMenu {
    public static void main(String[] args) throws Exception {
        for (String apk : args) try (var zip = new ZipFile(apk)) {
            int add = 0, dispatch = 0, activityDispatch = 0, old = 0;
            boolean activity = false, marker = zip.getEntry("assets/haiagaru/imgur-web/installed") != null;
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                    if (cls.getType().equals("Lapp/morphe/extension/chmate/ImgurUploadsActivity;")) activity = true;
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        for (var ins : method.getImplementation().getInstructions()) {
                            if (!(ins instanceof ReferenceInstruction r) || !(r.getReference() instanceof MethodReference m)
                                    || !m.getDefiningClass().equals("Lapp/morphe/extension/chmate/ImgurWebUpload;")) continue;
                            if (m.getName().equals("attachPostingButton")) old++;
                            if (cls.getType().startsWith("Lapp/morphe/extension/")) continue;
                            if (Set.of("addPostingMenu", "handlePostingMenu", "handlePostingActivityMenu").contains(m.getName())) {
                                if (!Set.of("Lo/p9ExternalSyntheticLambda6;", "Ljp/syoboi/a2chMate/feature/resedit/ResEditFragment;", "Lo/m6b;", "Lo/m6b$4;")
                                        .contains(cls.getType())) throw new AssertionError("Hook outside posting fragment");
                                if (m.getName().equals("addPostingMenu")) add++;
                                else if (m.getName().equals("handlePostingActivityMenu")) activityDispatch++;
                                else dispatch++;
                            }
                        }
                    }
                }
            }
            if (!marker || !activity || add < 1 || dispatch != 1 || old != 0 || (add == 2 && activityDispatch != 1))
                throw new AssertionError(apk + " add=" + add + " dispatch=" + dispatch + " old=" + old);
            System.out.println("PASS " + apk + " menuHooks=" + add + " dispatch=" + dispatch + " activityDispatch=" + activityDispatch + " oldButtonCalls=" + old);
        }
    }
}
