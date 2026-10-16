import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

/** Verifies actual patched callbacks and that lifecycle cleanup cannot directly close sockets. */
public final class VerifyImgurHistory {
    public static void main(String[] args) throws Exception {
        for (String apk : args) try (var zip = new ZipFile(apk)) {
            int hooks = 0, markers = 0, cleanups = 0;
            if (zip.getEntry("assets/haiagaru/imgur-web/installed") == null) throw new AssertionError("Missing option marker");
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses())
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        boolean lifecycle = cls.getType().equals("Lapp/morphe/extension/chmate/ImgurThumbnailLoader;")
                                && Set.of("reset", "close").contains(method.getName());
                        if (lifecycle) cleanups++;
                        for (var instruction : method.getImplementation().getInstructions()) {
                            if (!(instruction instanceof ReferenceInstruction r) || !(r.getReference() instanceof MethodReference m)) continue;
                            if (m.getDefiningClass().equals("Lapp/morphe/extension/chmate/ImgurUploadHistory;") && m.getName().equals("addNativeHistory")) {
                                if (!cls.getType().startsWith("Lo/")) throw new AssertionError("History hook outside native callback");
                                hooks++;
                            }
                            if (cls.getType().equals("Lapp/morphe/extension/chmate/ImgurWebUpload;") && method.getName().equals("upload")
                                    && m.getName().equals("uploaded") && m.getDefiningClass().endsWith("/ImgurUploadHistory;")) markers++;
                            if (lifecycle && Set.of("cancel", "cancelAll", "shutdownNow").contains(m.getName()))
                                throw new AssertionError("UI cleanup still cancels networking directly: " + m);
                            if (lifecycle && m.getName().equals("evictAll") && m.getDefiningClass().startsWith("Lokhttp3/"))
                                throw new AssertionError("UI cleanup still closes connection pool");
                            if (lifecycle && !m.getName().equals("<init>")
                                    && m.getDefiningClass().startsWith("Lapp/morphe/extension/isolated/"))
                                throw new AssertionError("UI cleanup calls shaded networking directly: " + m);
                        }
                    }
            }
            if (hooks != 1 || markers != 1 || cleanups != 2) throw new AssertionError("Counts: "+hooks+"/"+markers+"/"+cleanups);
            System.out.println("PASS native callback, successful-login marker and worker cleanup: " + apk);
        }
    }
}
