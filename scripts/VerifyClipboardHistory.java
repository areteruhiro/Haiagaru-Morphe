import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.*;

public final class VerifyClipboardHistory {
    public static void main(String[] args) throws Exception {
        for (String apk : args) {
            int nativeCopies=0, extensionCopies=0, posting=0, dispatch=0, recording=0, settings=0;
            boolean readsClipboard=false;
            try(var zip=new ZipFile(apk)) { for(var e:Collections.list(zip.entries())) {
                if(!e.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
                for(var c:new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) for(var m:c.getMethods()) {
                    if(m.getImplementation()==null)continue;
                    for(var i:m.getImplementation().getInstructions()) {
                        if(!(i instanceof ReferenceInstruction r) || !(r.getReference() instanceof MethodReference ref))continue;
                        if(c.getType().contains("ClipboardHistory") && ref.getName().equals("getPrimaryClip"))readsClipboard=true;
                        if(!ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/ClipboardHistory;"))continue;
                        switch(ref.getName()) {
                            case "setPrimaryClip": if(c.getType().startsWith("Lo/")||c.getType().startsWith("Ljp/")) nativeCopies++; else extensionCopies++; break;
                            case "addPostingMenu": posting++; break;
                            case "handlePostingMenu": if(!c.getType().contains("ClipboardHistory"))dispatch++; break;
                            case "addSettings": settings++; break;
                            case "save": recording++; break;
                        }
                    }
                }
            }}
            if(nativeCopies==0||extensionCopies==0||posting==0||dispatch==0||settings==0||recording==0||readsClipboard)
                throw new AssertionError(apk+": missing copy/menu/settings binding or unexpected clipboard reader "+nativeCopies+"/"+extensionCopies+"/"+posting+"/"+dispatch+"/"+settings+"/"+recording);
            System.out.println("PASS "+apk+": native="+nativeCopies+", extension="+extensionCopies+", menu="+posting+", dispatch="+dispatch+", settings="+settings+", no clipboard reads");
        }
    }
}
