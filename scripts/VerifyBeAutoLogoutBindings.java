import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.ZipFile;
public final class VerifyBeAutoLogoutBindings {
 public static void main(String[] args)throws Exception {
  for(String apk:args)try(var zip=new ZipFile(apk)) {
   int complete=0,editor=0,send=0;String worker=null,action=null,boardGetter=null;boolean safeCtor=false;
   for(var entry:Collections.list(zip.entries())) {
    if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;
    byte[] data;try(var in=zip.getInputStream(entry)){data=in.readAllBytes();}
    for(var cls:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(data)).getClasses())for(var m:cls.getMethods()) {
     if(m.getImplementation()==null)continue;
     if(cls.getType().equals("Lapp/morphe/extension/chmate/BeAutoLogout;")&&m.getName().equals("constructor"))safeCtor=true;
     for(var ins:m.getImplementation().getInstructions())if(ins instanceof ReferenceInstruction r) {
      if(cls.getType().equals("Lapp/morphe/extension/chmate/BeAutoLogout;")&&r.getReference() instanceof StringReference s) {
       if(m.getName().equals("nativeWorker"))worker=s.getString();
       if(m.getName().equals("nativeAction"))action=s.getString();
       if(m.getName().equals("nativeBoardUrl"))boardGetter=s.getString();
      }
      if(r.getReference() instanceof MethodReference ref&&ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/BeAutoLogout;")) {
       if(ref.getName().equals("completed"))complete++;
       if(ref.getName().equals("editor"))editor++;
       if(ref.getName().equals("beforeSend")) {
        send++;
        if(!m.getParameterTypes().get(1).equals("Ljp/syoboi/a2chMate/client/BBSUrlInfo;"))throw new AssertionError("Not a thread send method");
       }
      }
     }
    }
   }
   if(complete!=2||editor!=1||send!=1||worker==null||worker.isEmpty()||action==null||action.isEmpty()||boardGetter==null||boardGetter.isEmpty()||!safeCtor)
    throw new AssertionError(apk+" completed="+complete+" editor="+editor+" worker="+worker+" action="+action+" ctor="+safeCtor);
   System.out.println("PASS "+apk+" completed="+complete+" editor="+editor+" send="+send+" board="+boardGetter+" native="+worker+"->"+action);
  }
 }
}
