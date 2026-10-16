import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

/** APK path / on-or-off pairs. Manifest presence is also checked with aapt2. */
public final class VerifyVideoViewerOption {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || args.length % 2 != 0) throw new AssertionError("APK on/off pairs required");
        String viewer = "Lapp/morphe/extension/chmate/VideoViewerActivity;";
        for (int n = 0; n < args.length; n += 2) try (var zip = new ZipFile(args[n])) {
            var marker = zip.getEntry("assets/haiagaru/video-viewer/installed");
            if ((marker != null) != args[n+1].equals("on")) throw new AssertionError("Selection mismatch");
            if (marker != null) try (var in = zip.getInputStream(marker)) {
                if (in.read() != '1') throw new AssertionError("Invalid marker");
            }
            boolean markerCheck = false, settingsGate = false, entryGate = false;
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses())
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        for (var instruction : method.getImplementation().getInstructions()) {
                            if (!(instruction instanceof ReferenceInstruction r)) continue;
                            if (cls.getType().equals(viewer) && method.getName().equals("installed")
                                    && r.getReference() instanceof StringReference s)
                                markerCheck |= s.getString().equals("haiagaru/video-viewer/installed");
                            if (!(r.getReference() instanceof MethodReference m)) continue;
                            if (method.getName().equals("videoViewerEnabled"))
                                settingsGate |= m.getDefiningClass().equals(viewer) && m.getName().equals("installed");
                            if (cls.getType().equals(viewer) && method.getName().equals("onCreate"))
                                entryGate |= m.getName().equals("videoViewerEnabled");
                        }
                    }
            }
            if (!markerCheck || !settingsGate || !entryGate) throw new AssertionError("Missing gate");
            System.out.println("PASS " + args[n] + " option=" + args[n+1] + " marker/settings/activity gates");
        }
    }
}
