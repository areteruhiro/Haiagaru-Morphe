import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;
public final class InspectImgurHistory {
 public static void main(String[] args)throws Exception{
  for(String apk:args)try(var zip=new ZipFile(apk)){
   System.out.println("APK "+apk);
   var rows=new HashSet<String>();
   var all=new ArrayList<com.android.tools.smali.dexlib2.iface.ClassDef>();
   for(var e:Collections.list(zip.entries())){
    if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
    byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
    for(var c:new DexBackedDexFile(Opcodes.getDefault(),ByteBuffer.wrap(bytes)).getClasses()){
     all.add(c);
     var names=new HashSet<String>();c.getFields().forEach(f->names.add(f.getName()));
     if(names.containsAll(Set.of("link","target","source","key"))){System.out.println("ROW "+c.getType());rows.add(c.getType());}
    }
   }
   for(var c:all){
     for(var m:c.getMethods()){
      if(m.getImplementation()==null)continue;
      boolean history=false,post=false;
      for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r){
       if(r.getReference() instanceof StringReference s && s.getString().equals("upload.json"))history=true;
       if(r.getReference() instanceof MethodReference ref && ref.getName().equals("<init>")&&rows.contains(ref.getDefiningClass()))post=true;
      }
      if(history)System.out.println("FILE "+m);
      if(post){System.out.println("CALLBACK "+m);for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r)System.out.println(" "+r.getReference());}
     }
    }
  }
 }
}
