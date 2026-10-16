import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

/** Read-only inspection of cache preference consumers and byte-limit arithmetic. */
public final class InspectImageCache {
 public static void main(String[] args) throws Exception {
  try (var zip = new ZipFile(args[0])) {
   for (var e : Collections.list(zip.entries())) {
    if (!e.getName().matches("classes[0-9]*\\.dex")) continue;
    byte[] bytes; try (var in = zip.getInputStream(e)) { bytes = in.readAllBytes(); }
    for (var c : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses()) {
     for (var m : c.getMethods()) {
      if (m.getImplementation() == null) continue;
      boolean found = args.length > 1 && c.getType().equals(args[1]);
      for (var i : m.getImplementation().getInstructions()) {
       if (i instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s
           && s.getString().equals("imageCacheSize")) found = true;
      }
      if (!found) continue;
      System.out.println("METHOD " + m);
      for (var i : m.getImplementation().getInstructions()) {
       System.out.print(" " + i.getOpcode());
       if (i instanceof ReferenceInstruction r) System.out.print(" " + r.getReference());
       if (i instanceof WideLiteralInstruction n) System.out.print(" " + n.getWideLiteral());
       System.out.println();
      }
     }
    }
   }
  }
 }
}
