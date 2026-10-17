import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.io.*;
import java.util.zip.*;

/** Checks patched native text bindings and dispatch wrappers, not actual UI gestures. */
public final class VerifyXPostPreviewHooks {
    public static void main(String[] args) throws Exception {
        int bindings=0,touches=0;
        try(ZipFile apk=new ZipFile(args[0])) {
            for(var entries=apk.entries();entries.hasMoreElements();) {
                var entry=entries.nextElement();
                if(!entry.getName().matches("classes(?:[0-9]+)?\\.dex"))continue;
                try(var input=new BufferedInputStream(apk.getInputStream(entry))) {
                    for(var cls:DexBackedDexFile.fromInputStream(Opcodes.getDefault(),input).getClasses()) {
                        if(!cls.getType().startsWith("Lo/")&&!cls.getType().startsWith("Ljp/syoboi/"))continue;
                        for(var method:cls.getMethods())if(method.getImplementation()!=null)
                            for(var instruction:method.getImplementation().getInstructions())
                                if(instruction instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference ref) {
                                    if(ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/XPostPreview;")&&ref.getName().equals("onBound"))bindings++;
                                    if(ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/XPostPreviewTouch;")&&ref.getName().equals("onTouch")) {
                                        if(!method.getName().equals("dispatchTouchEvent"))throw new AssertionError("Unexpected dispatcher");
                                        System.out.println("TOUCH "+cls.getType()+" extends "+cls.getSuperclass());touches++;
                                    }
                                }
                    }
                }
            }
        }
        if(bindings==0||touches==0)throw new AssertionError("Missing preview hooks");
        System.out.println("PASS "+bindings+" native text bindings, "+touches+" native touch dispatchers");
    }
}
