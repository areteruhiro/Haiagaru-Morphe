import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.*;

/** All patrol entry points must share the download gate; ordinary reads are untouched. */
public class VerifyPatrolImageHooks {
    public static void main(String[] args) throws Exception {
        int gates=0, callbacks=0, wrappers=0;
        try(var zip=new ZipFile(args[0])) {
            for(var entry:Collections.list(zip.entries())) {
                if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;
                byte[] bytes;try(var in=zip.getInputStream(entry)){bytes=in.readAllBytes();}
                for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses())
                    for(var m:c.getMethods()) {
                        if(m.getName().equals("haiagaruPatrolImages"))wrappers++;
                        if(m.getImplementation()==null)continue;
                        var instructions=new ArrayList<Instruction>();m.getImplementation().getInstructions().forEach(instructions::add);
                        for(int n=0;n<instructions.size();n++) {
                            var i=instructions.get(n);
                            if(!(i instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference ref))continue;
                            if(!ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/PatrolImages;"))continue;
                            if(ref.getName().equals("needsDat")) {
                                if(!c.getType().equals("Ljp/syoboi/a2chMate/autocheck/AutoCheckWork;")
                                        || n!=0 || !m.getParameterTypes().get(2).equals("Z"))throw new AssertionError("Unscoped gate: "+m);
                                var call=(RegisterRangeInstruction)i;
                                if(call.getRegisterCount()!=1 || call.getStartRegister()!=m.getImplementation().getRegisterCount()-1
                                        || ((OneRegisterInstruction)instructions.get(1)).getRegisterA()!=call.getStartRegister())
                                    throw new AssertionError("Download flag register");
                                gates++;
                            }
                            if(ref.getName().equals("onThread") && !c.getType().startsWith("Lapp/morphe/extension/")) {
                                if(!m.getName().equals("haiagaruPatrolImages"))throw new AssertionError("Unscoped image callback");
                                callbacks++;
                            }
                        }
                    }
            }
        }
        if(gates!=1||callbacks!=1||wrappers!=1)throw new AssertionError(gates+"/"+callbacks+"/"+wrappers);
        System.out.println("PASS shared startup/manual/periodic patrol gate and DAT callback: "+args[0]);
    }
}
