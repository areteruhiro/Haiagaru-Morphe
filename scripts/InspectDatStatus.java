import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;import java.util.zip.*;
public class InspectDatStatus {
 public static void main(String[] a)throws Exception{try(var z=new ZipFile(a[0])){for(var e:Collections.list(z.entries()))if(e.getName().matches("classes[0-9]*\\.dex"))try(var in=z.getInputStream(e)){for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses())if(Arrays.asList(a).contains(c.getType())){
  System.out.println("CLASS "+c.getType()+" super="+c.getSuperclass());for(var f:c.getFields())System.out.println("FIELD "+f);
  for(var m:c.getMethods())if(m.getImplementation()!=null){System.out.println("METHOD "+m);for(var i:m.getImplementation().getInstructions())System.out.println(" "+i.getOpcode()+(i instanceof OneRegisterInstruction r?" v"+r.getRegisterA():"")+(i instanceof TwoRegisterInstruction r?",v"+r.getRegisterB():"")+(i instanceof ReferenceInstruction r?" "+r.getReference():"")+(i instanceof NarrowLiteralInstruction r?" #"+r.getNarrowLiteral():""));}
 }}}}
}
