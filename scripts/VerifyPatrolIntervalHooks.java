import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.*;
import java.util.zip.ZipFile;

public class VerifyPatrolIntervalHooks {
    public static void main(String[] args)throws Exception {
        String[] owners={"Lo/mdExternalSyntheticLambda0;","Lo/settings;","Lo/TTRewardVideoActivity8;"};
        for(int n=0;n<args.length;n++) {
            List<ClassDef> classes=new ArrayList<>();
            try(var zip=new ZipFile(args[n])) {
                for(var entry:Collections.list(zip.entries()))if(entry.getName().matches("classes[0-9]*\\.dex"))
                    try(var in=zip.getInputStream(entry)){new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses().forEach(classes::add);}
            }
            String expected=owners[n];int helpers=0,calls=0,platformPeriodic=0;
            for(var c:classes)for(var m:c.getMethods()) {
                if(m.getName().equals("haiagaruPatrolInterval")) {
                    if(!c.getType().equals(expected)||!m.getReturnType().equals("V")||m.getParameterTypes().size()!=2)throw new AssertionError(m);
                    helpers++;
                }
                if(m.getImplementation()==null)continue;
                for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference ref) {
                    if(ref.getName().equals("haiagaruPatrolInterval")) {
                        if(!c.getType().equals(expected)||!m.getName().equals("e")||!m.getParameterTypes().equals(List.of("Landroid/content/Context;")))throw new AssertionError("Unscoped hook: "+m);
                        calls++;
                    }
                    if(ref.getDefiningClass().equals("Landroid/app/job/JobInfo$Builder;")&&ref.getName().equals("setPeriodic"))platformPeriodic++;
                }
            }
            if(helpers!=1||calls!=1||platformPeriodic!=0)throw new AssertionError(args[n]+" helpers="+helpers+" calls="+calls+" platformPeriodic="+platformPeriodic);
            System.out.println("PASS patrol hook scope and latency-based scheduling: "+args[n]);
        }
    }
}
