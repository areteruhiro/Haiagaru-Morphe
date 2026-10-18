import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.ZipFile;
public class InspectArchiveSorting {
 public static void main(String[] args)throws Exception {
  try(var z=new ZipFile(args[0])) {
   var classes=new ArrayList<com.android.tools.smali.dexlib2.iface.ClassDef>();
   for(var e:Collections.list(z.entries()))if(e.getName().matches("classes[0-9]*\\.dex"))try(var in=z.getInputStream(e)) {
    new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses().forEach(classes::add);
   }
   String item=null;
   for(var c:classes)for(var m:c.getMethods())if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions())
    if(i instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s && s.getString().equals("^(.*)\\[(\\d+)]$"))item=c.getType();
   System.out.println("ITEM "+item);
   for(var c:classes){boolean related=c.getType().equals(item);
    for(var f:c.getFields())if(f.getType().equals(item))related=true;
    for(var m:c.getMethods())if(m.getParameterTypes().contains(item))related=true;
    if(c.getType().equals("Lo/setAdCreativeClickListener;") || c.getType().equals("Lo/AndroidLogHandler;"))related=true;
    for(var m:c.getMethods())if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r) {
     var ref=r.getReference(); if(ref instanceof MethodReference a && (a.getDefiningClass().equals(item)||a.getParameterTypes().contains(item)))related=true;
    }
    if(!related)continue;
    System.out.println("CLASS "+c.getType()+" SUPER "+c.getSuperclass());
    for(var f:c.getFields())System.out.println(" FIELD "+f.getName()+" "+f.getType());
    for(var m:c.getMethods())if(m.getImplementation()!=null){System.out.println(" METHOD "+m.getName()+m.getParameterTypes()+m.getReturnType());
     for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r && (r.getReference() instanceof MethodReference ||r.getReference() instanceof StringReference))System.out.println("  "+i.getOpcode()+" "+r.getReference());
    }
   }
  }
 }
}
