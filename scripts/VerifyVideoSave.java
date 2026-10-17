import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.*;

public final class VerifyVideoSave {
    public static void main(String[] args)throws Exception {
        boolean label=false,confirm=false,enqueue=false,permission=false,worker=false;
        try(var zip=new ZipFile(args[0])) {for(var e:Collections.list(zip.entries())) {
            if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
            byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
            for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses())for(var m:c.getMethods()) {
                if(m.getImplementation()==null)continue;
                for(var i:m.getImplementation().getInstructions()) {
                    if(!(i instanceof ReferenceInstruction r))continue;
                    if(r.getReference() instanceof StringReference s && s.getString().equals("動画を保存") && c.getType().endsWith("/VideoViewerActivity;"))label=true;
                    if(!(r.getReference() instanceof MethodReference ref))continue;
                    if(c.getType().endsWith("/VideoViewerActivity;")) {
                        if(ref.getDefiningClass().endsWith("/VideoDownloads;") && ref.getName().equals("confirm"))confirm=true;
                        if(ref.getDefiningClass().endsWith("/VideoDownloads;") && ref.getName().equals("enqueue"))enqueue=true;
                    }
                    // D8 may outline API-23 calls into an isolated helper for minSdk 21.
                    if(c.getType().startsWith("Lapp/morphe/extension/") && ref.getName().equals("requestPermissions"))permission=true;
                    if(c.getType().endsWith("/VideoDownloads;") && ref.getName().equals("execute"))worker=true;
                    if(c.getType().endsWith("/YouTubeViewerActivity;") && ref.getDefiningClass().endsWith("/VideoDownloads;"))throw new AssertionError("YouTube must not download");
                }
            }
        }}
        if(!label||!confirm||!enqueue||!permission||!worker)throw new AssertionError("Missing save menu/confirmation/permission/worker binding");
        System.out.println("PASS viewer save menu/confirmation/permission/worker + YouTube exclusion");
    }
}
