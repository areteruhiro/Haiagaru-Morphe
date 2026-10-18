import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.*;
public class VerifyThreadArchiveHooks {
 public static void main(String[] args)throws Exception{
  for(String apk:args){int sort=0,compare=0,settings=0;
   try(var z=new ZipFile(apk)){for(var e:Collections.list(z.entries()))if(e.getName().matches("classes[0-9]*\\.dex"))try(var in=z.getInputStream(e)){
    for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses())for(var m:c.getMethods())if(m.getImplementation()!=null)
     for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference ref && ref.getDefiningClass().endsWith("/ThreadArchiveOrder;")){
      if(c.getType().startsWith("Lo/")&&ref.getName().equals("sort"))sort++;
      if(c.getType().startsWith("Lo/")&&ref.getName().equals("compare"))compare++;
      if(ref.getName().equals("addSettings"))settings++;
     }
   }}
   if(sort!=1||settings!=1||compare!=(apk.contains("226")?0:1))throw new AssertionError(apk+" hooks "+sort+"/"+compare+"/"+settings);
   System.out.println("PASS "+apk+" constructor-sort="+sort+" comparator="+compare+" settings="+settings);
  }
 }
}
