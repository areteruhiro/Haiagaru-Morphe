import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.ZipFile;
public final class InspectBeAutoLogout {
 public static void main(String[] args)throws Exception {
  for(String apk:args)try(var zip=new ZipFile(apk)) {
   int workers=0,hooks=0;
   for(var entry:Collections.list(zip.entries())) {
    if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;
    byte[] data;try(var in=zip.getInputStream(entry)){data=in.readAllBytes();}
    for(var cls:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(data)).getClasses())for(var m:cls.getMethods()) {
     if(m.getImplementation()==null)continue;
     var strings=new ArrayList<String>();var refs=new ArrayList<Reference>();
     for(var ins:m.getImplementation().getInstructions())if(ins instanceof ReferenceInstruction r){
      if(r.getReference() instanceof StringReference s)strings.add(s.getString());
      if(r.getReference() instanceof MethodReference || r.getReference() instanceof FieldReference)refs.add(r.getReference());
     }
     if(strings.stream().anyMatch(s->s.contains("logout.php"))&&strings.stream().anyMatch(s->s.contains("logout.html"))){System.out.println("WORKER "+m);for(var ref:refs)System.out.println("REF "+ref);workers++;}
     if((cls.getType().startsWith("Ljp/syoboi/a2chMate/feature/resedit/")||cls.getType().startsWith("Lo/p9ExternalSyntheticLambda6$"))
       && refs.stream().anyMatch(r->r instanceof MethodReference mr&&mr.getName().equals("setResult")&&mr.getParameterTypes().equals(List.of("I")))) {
      System.out.println("SUCCESS "+m);hooks++;
     }
    }
   }
   if(workers!=1||hooks<1)throw new AssertionError(apk+" workers="+workers+" success="+hooks);
   System.out.println("PASS native BE logout and new-thread success contract: "+apk);
  }
 }
}
