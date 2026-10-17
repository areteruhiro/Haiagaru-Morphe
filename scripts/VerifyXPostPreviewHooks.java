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
        boolean guardedBinding=false;
        try(ZipFile apk=new ZipFile(args[0])) {
            for(var entries=apk.entries();entries.hasMoreElements();) {
                var entry=entries.nextElement();
                if(!entry.getName().matches("classes(?:[0-9]+)?\\.dex"))continue;
                try(var input=new BufferedInputStream(apk.getInputStream(entry))) {
                    for(var cls:DexBackedDexFile.fromInputStream(Opcodes.getDefault(),input).getClasses()) {
                        if(cls.getType().equals("Lapp/morphe/extension/chmate/XPostPreview;"))
                            for(var method:cls.getMethods())if(method.getName().equals("onBound")&&method.getImplementation()!=null) {
                                boolean guardRead=false;
                                for(var instruction:method.getImplementation().getInstructions())
                                    if(instruction instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference ref) {
                                        if(ref.getDefiningClass().equals("Ljava/lang/ThreadLocal;")&&ref.getName().equals("get"))guardRead=true;
                                        if(ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/DirectBodySelection;")&&ref.getName().equals("prepare")) {
                                            if(!guardRead)throw new AssertionError("Native binding recurses before write guard");
                                            guardedBinding=true;
                                        }
                                    }
                            }
                        if(!cls.getType().startsWith("Lo/")&&!cls.getType().startsWith("Ljp/syoboi/"))continue;
                        for(var method:cls.getMethods())if(method.getImplementation()!=null)
                            for(var instruction:method.getImplementation().getInstructions())
                                if(instruction instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference ref) {
                                    if(ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/XPostPreview;")&&ref.getName().equals("onBound"))bindings++;
                                    if(ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/XPostPreviewTouch;")&&ref.getName().equals("onTouch")) {
                                        if(!method.getName().equals("dispatchTouchEvent")
                                                && !method.getName().equals("haiagaruDirectSelectionNativeDispatch"))
                                            throw new AssertionError("Unexpected dispatcher: "+method.getName());
                                        System.out.println("TOUCH "+cls.getType()+" extends "+cls.getSuperclass());touches++;
                                    }
                                }
                    }
                }
            }
        }
        if(bindings==0||touches==0||!guardedBinding)throw new AssertionError("Missing guarded preview hooks");
        System.out.println("PASS "+bindings+" native text bindings, "+touches+" native touch dispatchers, write reentry guard");
    }
}
