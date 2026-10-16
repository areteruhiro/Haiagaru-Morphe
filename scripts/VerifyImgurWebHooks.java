import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.*;
import java.util.zip.ZipFile;

public final class VerifyImgurWebHooks {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static void field(Map<String,ClassDef> classes, String type, String name, String expected) {
        ClassDef cls = classes.get(type); check(cls != null, "missing " + type);
        for (Field field : cls.getFields()) if (field.getName().equals(name) && field.getType().equals(expected)) return;
        throw new AssertionError(type + " field mismatch: " + name + " " + expected);
    }
    public static void main(String[] args) throws Exception {
        boolean without = Arrays.asList(args).contains("--without");
        for (String apk : args) {
            if (apk.equals("--without")) continue;
            Map<String,ClassDef> classes = new HashMap<>();
            try (ZipFile zip = new ZipFile(apk)) {
                check((zip.getEntry("assets/haiagaru/imgur-web/installed") != null) != without,
                        "opt-in marker does not match selected patch: " + apk);
                for (var entry : Collections.list(zip.entries())) {
                    if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                    try (var in = zip.getInputStream(entry)) {
                        for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses()) classes.put(cls.getType(), cls);
                    }
                }
            }
            String response, data, dataField, success, status, entryHook;
            if (apk.contains("191")) { response="Lo/r0ExternalSyntheticLambda13;"; data="Lo/r0ExternalSyntheticLambda16;"; dataField="d"; success="b"; status="c"; entryHook="uploadLegacyImage"; }
            else if (apk.contains("226")) { response="Lo/Ad;"; data="Lo/DN;"; dataField="d"; success="c"; status="b"; entryHook="invokePreIoImageUploader"; }
            else if (apk.contains("241")) { response="Lo/defaultzzb;"; data="Lo/zzadx;"; dataField="c"; success="e"; status="d"; entryHook="invoke241"; }
            else { response="Lo/zzbtj;"; data="Lo/zzbtn;"; dataField="a"; success="c"; status="e"; entryHook="invoke242"; }
            field(classes, response, dataField, data);
            String base = classes.get(response).getSuperclass();
            field(classes, base, success, "Z"); field(classes, base, status, "I");
            if (apk.contains("191")) { field(classes,data,"b","Ljava/lang/String;"); field(classes,data,"d","Ljava/lang/String;"); }
            else {
                List<String> expected = List.of("Ljava/lang/String;","Ljava/lang/String;","Ljava/lang/String;","Ljava/lang/String;","Z","I","I","I","I","I","Ljava/lang/String;","Z","Ljava/lang/String;","Ljava/lang/String;","Ljava/lang/String;","Ljava/lang/String;","Ljava/lang/String;");
                boolean constructor = false;
                for (Method method : classes.get(data).getMethods()) if (method.getName().equals("<init>") && method.getParameterTypes().stream().map(Object::toString).toList().equals(expected)) constructor = true;
                check(constructor, "native image constructor changed");
            }
            check(classes.containsKey("Lapp/morphe/extension/chmate/ImgurWebLoginActivity;"), "login activity missing");
            ClassDef helper = classes.get("Lapp/morphe/extension/chmate/ImgurWebUpload;");
            check(helper != null, "upload helper missing");
            int hooks=0;
            for (ClassDef cls : classes.values()) for (Method method : cls.getMethods()) {
                if (method.getImplementation() == null) continue;
                for (var ins : method.getImplementation().getInstructions()) if (ins instanceof ReferenceInstruction ref && ref.getReference() instanceof MethodReference target
                        && target.getName().equals(entryHook) && !cls.getType().startsWith("Lapp/morphe/extension/")) hooks++;
            }
            int expectedHooks = without && !apk.contains("191") && !apk.contains("226") ? 0 : 1;
            check(hooks == expectedHooks, apk + " expected " + expectedHooks + " native uploader hooks, got " + hooks);
            System.out.println("PASS: " + apk + " native response fields, constructor, helper and upload hook");
        }
    }
}
