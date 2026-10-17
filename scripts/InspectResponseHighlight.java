import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.*;
public final class InspectResponseHighlight {
 public static void main(String[] args)throws Exception{
  try(var zip=new ZipFile(args[0])){for(var e:Collections.list(zip.entries())){
   if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
   byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
   for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses()){
    for(var m:c.getMethods()){
     if(m.getImplementation()==null || !m.getReturnType().equals("Landroid/text/Spanned;"))continue;
     if(m.getParameterTypes().stream().noneMatch(p->p.toString().equals(args[1])))continue;
     System.out.println("METHOD "+m+" registers="+m.getImplementation().getRegisterCount());
     c.getFields().forEach(f->System.out.println("FIELD "+f));
     int i=0;for(var op:m.getImplementation().getInstructions()){
      if(op instanceof ReferenceInstruction r)System.out.println(i+" "+op.getOpcode()+" "+r.getReference());
      i++;
     }
    }
   }
  }}
 }
}
