import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import com.android.tools.smali.dexlib2.iface.reference.FieldReference;
import com.android.tools.smali.dexlib2.iface.reference.StringReference;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import java.util.zip.ZipFile;
import java.nio.ByteBuffer;
public class InspectToolbarIcons {
 public static void main(String[] args)throws Exception {
  try(var zip=new ZipFile(args[0])) {
   var entries=zip.entries();while(entries.hasMoreElements()) {
    var e=entries.nextElement();if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
    try(var in=zip.getInputStream(e)) {
     for(var c:new DexBackedDexFile(Opcodes.getDefault(),ByteBuffer.wrap(in.readAllBytes())).getClasses()) {
      for(var m:c.getMethods()) if(m.getReturnType().equals("Landroid/graphics/drawable/Drawable;") && m.getParameterTypes().toString().equals("[Landroid/content/Context;, I]")) System.out.println("DRAW "+m);
      if(c.getType().toLowerCase().contains("toolbar"))System.out.println("TYPE "+c.getType());
      for(var m:c.getMethods())if(m.getImplementation()!=null)for(var i:m.getImplementation().getInstructions()) {
       if(i instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s &&
        (s.getString().contains("ToolbarCustomize") || s.getString().equals("defaultSettings"))) System.out.println("TARGET "+m+" -> "+s.getString());
       if(i instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference ref &&
        ref.getDefiningClass().contains("ToolbarButtonSpec"))System.out.println("USE "+m+" -> "+ref);
       if(i instanceof ReferenceInstruction r && r.getReference() instanceof FieldReference ref &&
        ref.getDefiningClass().contains("ToolbarButtonSpec"))System.out.println("FIELD "+m+" -> "+ref);
      }
     }
    }
   }
  }
 }
}
