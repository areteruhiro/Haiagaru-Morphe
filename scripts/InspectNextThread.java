import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;import java.util.zip.*;
public class InspectNextThread {
 public static void main(String[] args)throws Exception{for(String apk:args)try(var z=new ZipFile(apk)){
  System.out.println(apk);
  for(var e:Collections.list(z.entries()))if(e.getName().matches("classes[0-9]*\\.dex"))try(var in=z.getInputStream(e)){
   for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses())for(var m:c.getMethods())if(m.getImplementation()!=null){
    boolean match=false;for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference s&&s.getString().equals("relUrl"))match=true;
    if(args.length==1){match=false;for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r&&r.getReference() instanceof FieldReference s&&s.getDefiningClass().equals("Lo/zzatp;")&&s.getName().equals("b"))match=true;}
    if(!match)continue;System.out.println(m+" flags="+m.getAccessFlags()+" regs="+m.getImplementation().getRegisterCount());
    for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r)System.out.println(" "+i.getOpcode()+" "+r.getReference());
   }
  }
 }}
}
