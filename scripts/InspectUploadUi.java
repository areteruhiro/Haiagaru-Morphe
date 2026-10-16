import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.ZipFile;
public final class InspectUploadUi {
 public static void main(String[] args)throws Exception{
  try(var zip=new ZipFile(args[0])){
   for(var e:Collections.list(zip.entries())){
    if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
    byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
    for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses()){
     boolean hit=false;
     for(var m:c.getMethods())if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r){
      var ref=r.getReference();
      if(ref instanceof FieldReference f && f.getDefiningClass().equals(args[1]))hit=true;
      if(ref instanceof StringReference s && (s.getString().contains("api.imgur.com")||s.getString().contains("Client-ID")))hit=true;
     }
     if(!hit)continue;
     System.out.println("CLASS "+c.getType()+" super="+c.getSuperclass());
     for(var f:c.getFields())System.out.println(" FIELD "+f);
     for(var m:c.getMethods()){
      System.out.println(" METHOD "+m);
      if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s)System.out.println(" STRING "+s.getString());
     }
    }
   }
  }
 }
}
