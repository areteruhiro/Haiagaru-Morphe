import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.Method;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

/** Checks the actual signed APK, not just the Kotlin patch source. */
public final class VerifyLegacyAutoReloadHooks {
    private static final String EXT = "Lapp/morphe/extension/chmate/LegacyAutoReload;";
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static int calls(Method method, String owner, String name) {
        return calls(method, owner, name, -1);
    }
    private static int calls(Method method, String owner, String name, int parameters) {
        int count = 0;
        for (var instruction : method.getImplementation().getInstructions()) {
            if (instruction instanceof ReferenceInstruction ref && ref.getReference() instanceof MethodReference target
                    && target.getDefiningClass().equals(owner) && target.getName().equals(name)
                    && (parameters < 0 || (target.getParameterTypes().size() == parameters
                        && target.getReturnType().equals("V")))) count++;
        }
        return count;
    }
    private static Method method(ClassDef owner, String name, List<String> parameters) {
        List<Method> matches = new ArrayList<>();
        for (Method method : owner.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().equals(parameters)) matches.add(method);
        }
        check(matches.size() == 1, owner.getType() + "->" + name + " ambiguous/missing");
        return matches.get(0);
    }
    public static void main(String[] args) throws Exception {
        for (String apk : args) {
            ClassDef fragment = null, extension = null;
            try (ZipFile zip = new ZipFile(apk)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                    byte[] bytes;
                    try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                    for (ClassDef cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) {
                        if (cls.getType().equals("Ljp/syoboi/a2chMate/ui/reslist/ResListFragment;")) fragment = cls;
                        if (cls.getType().equals(EXT)) extension = cls;
                    }
                }
            }
            check(fragment != null && extension != null, apk + " required classes missing");
            Method longClick = method(fragment, "onLongClick", List.of("Landroid/view/View;"));
            boolean v241 = longClick.getImplementation().getRegisterCount() == 16;
            check(calls(longClick, EXT, "start") == 2, "Both stock start branches redirected");
            check(calls(longClick, EXT, "stopLongPress") == 1, "Long-press toggle retained");
            check(calls(longClick, EXT, "intervalSeconds") == 2, "Both default timing branches configurable");
            check(calls(longClick, EXT, "intervalForBoard") == 1, "Board timing branch protected");
            check(calls(longClick, v241 ? "Lo/getAvailableFeatures;" : "Lo/zzaeg;", "d", 1) == 0,
                    "Long-press must not start native tailing");
            check(calls(method(fragment, "onPause", List.of()), EXT, "stop") == 1, "Pause stops timer");
            check(calls(method(fragment, "onDestroyView", List.of()), EXT, "stop") == 1, "Destroy stops timer");
            check(calls(method(fragment, v241 ? "a" : "e", List.of("Z")), EXT, "activeTab") == 1,
                    "Tab exit stops timer");
            check(calls(method(fragment, v241 ? "e" : "b", List.of("Z")), EXT, "scrollState") == 1,
                    "Native auto-scroll callback connected");
            check(calls(method(fragment, v241 ? "a" : "e", List.of(fragment.getType(),
                    v241 ? "Lo/honorsDebugCertificates;" : "Lo/zzacz;")), EXT, "event") == 1,
                    "Native reload-result callback connected");
            method(extension, "start", List.of("Ljava/lang/Object;", "J"));
            method(extension, "stop", List.of("Ljava/lang/Object;"));
            System.out.println(apk + ": all " + (v241 ? "241" : "242") + " auto-reload hooks verified");
        }
    }
}
