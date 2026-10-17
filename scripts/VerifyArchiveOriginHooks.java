import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.*;

public final class VerifyArchiveOriginHooks {
    public static void main(String[] args)throws Exception {
        for(String apk:args) {
            int copies=0,observe=0,constructor=0,settings=0; boolean root=false,flush=false,nativeFile=false,mega=false;
            try(var zip=new ZipFile(apk)){for(var e:Collections.list(zip.entries())){
                if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
                byte[] bytes;try(var input=zip.getInputStream(e)){bytes=input.readAllBytes();}
                for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses())for(var m:c.getMethods()) {
                    if(m.getImplementation()==null)continue;
                    for(var i:m.getImplementation().getInstructions()){
                        if(!(i instanceof ReferenceInstruction r))continue;
                        if(c.getType().endsWith("/HaiagaruBackupArchive;") && r.getReference() instanceof StringReference s && s.getString().equals("archive-origins"))root=true;
                        if(!(r.getReference() instanceof MethodReference ref))continue;
                        if(c.getType().endsWith("/HaiagaruMegaSync;") && ref.getDefiningClass().endsWith("/HaiagaruBackupArchive;") && ref.getName().equals("capture"))mega=true;
                        if(c.getType().endsWith("/BBSUrlInfo;") && m.getName().contains("haiagaruNativeArchiveUrl") && ref.getName().equals("fromFile"))nativeFile=true;
                        if(!ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/ArchiveOrigins;"))continue;
                        if(ref.getName().equals("copied")&&c.getType().startsWith("Lo/"))copies++;
                        if(c.getType().endsWith("/BBSUrlInfo;")&&ref.getName().equals("observe"))observe++;
                        if(c.getType().endsWith("/BBSUrlInfo;")&&m.getName().equals("<init>")&&ref.getName().equals("onModel"))constructor++;
                        if(ref.getName().equals("addSettings"))settings++;
                        if(c.getType().endsWith("/HaiagaruBackupArchive;")&&ref.getName().equals("flush"))flush=true;
                    }
                }
            }}
            if(copies!=2||observe!=1||constructor!=1||settings!=2||!root||!flush||!nativeFile||!mega)
                throw new AssertionError(apk+": "+copies+"/"+observe+"/"+constructor+"/"+settings+"/"+root+"/"+flush+"/"+nativeFile+"/"+mega);
            System.out.println("PASS "+apk+": native archive copy/URL observation, two UI entries, portable backup root, pending-write flush, MEGA path, native file routing unchanged");
        }
    }
}
