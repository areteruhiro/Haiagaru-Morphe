import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;import java.util.zip.*;
public class InspectPopular {
 public static void main(String[] a)throws Exception {try(var z=new ZipFile(a[0])){
  for(var e:Collections.list(z.entries()))if(e.getName().matches("classes[0-9]*\\.dex"))try(var in=z.getInputStream(e)){
   for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses()){
    boolean target=Arrays.asList(a).contains(c.getType());
    if(target){System.out.println("CLASS "+c.getType());for(var f:c.getFields())System.out.println("FIELD "+f);}
    for(var m:c.getMethods())if(m.getImplementation()!=null){boolean popular=false;
     for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r && r.getReference() instanceof FieldReference f&&f.getName().equals("POPULAR"))popular=true;
     if(!target&&!popular)continue; if(target&&!popular&& !a[a.length-1].equals("full"))continue;
     System.out.println("METHOD "+m+" regs="+m.getImplementation().getRegisterCount());
     for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r)System.out.println(" "+i.getOpcode()+" "+r.getReference());
    }
   }
  }
 }}
}
