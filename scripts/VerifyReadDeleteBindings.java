import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.*;
import java.util.zip.ZipFile;
import java.nio.charset.StandardCharsets;
public final class VerifyReadDeleteBindings {
 public static void main(String[] args)throws Exception {
  for(String apk:args)try(var zip=new ZipFile(apk)) {
   int longHooks=0,bindHooks=0,gesture=0,icons=0,xml=0;
   for(var entry:Collections.list(zip.entries())) {
    byte[] bytes;try(var input=zip.getInputStream(entry)){bytes=input.readAllBytes();}
    if(entry.getName().endsWith(".xml")&&(new String(bytes,StandardCharsets.UTF_8).contains("haiagaruGestureDeleteReadThreads")||new String(bytes,StandardCharsets.UTF_16LE).contains("haiagaruGestureDeleteReadThreads")))xml++;
    if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;
    for(var cls:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses())for(var method:cls.getMethods()) {
     if(method.getImplementation()==null||cls.getType().startsWith("Lapp/morphe/extension/"))continue;
     for(var ins:method.getImplementation().getInstructions())if(ins instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m) {
      if(m.getDefiningClass().equals("Lapp/morphe/extension/chmate/DeleteReadThreads;")) {
       if(m.getName().equals("longClick")){if(!method.getName().equals("onLongClick"))throw new AssertionError(method);longHooks++;}
       if(m.getName().equals("bind")){if(!method.getName().equals("onViewCreated"))throw new AssertionError(method);bindHooks++;}
      }
      if(m.getDefiningClass().equals("Lapp/morphe/extension/chmate/NgEditGesture;")&&m.getName().equals("onGesture")&&m.getParameterTypes().size()==2)gesture++;
      if(m.getDefiningClass().equals("Lapp/morphe/extension/chmate/ToolbarIcons;")&&m.getName().equals("replaceNative"))icons++;
     }
    }
   }
   if(longHooks!=1||bindHooks<1||gesture!=1||icons<1||xml!=1)throw new AssertionError(apk+" long="+longHooks+" bind="+bindHooks+" gesture="+gesture+" icons="+icons+" xml="+xml);
   if(zip.getEntry("assets/haiagaru/licenses/material-icons/LICENSE")==null)throw new AssertionError("Missing Material license");
   System.out.println("PASS "+apk+" refreshLongPress="+longHooks+" lifecycle="+bindHooks+" gesture="+gesture+" iconReturns="+icons+" preference="+xml);
  }
 }
}
