import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

public class VerifyWacchoiSuffixHooks {
    public static void main(String[] args) throws Exception {
        int rows=0,compose=0;
        String expected=args[1].equals("226")?"suffixCombinedClickable5":"suffixCombinedClickable";
        try(var zip=new ZipFile(args[0])) {
            for(var e:Collections.list(zip.entries())) {
                if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
                byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
                for(var c:new DexBackedDexFile(Opcodes.getDefault(),ByteBuffer.wrap(bytes)).getClasses()) {
                    if(c.getType().startsWith("Lapp/morphe/extension/"))continue;
                    for(var m:c.getMethods()) {
                        if(m.getImplementation()==null)continue;
                        for(var i:m.getImplementation().getInstructions()) {
                            if(!(i instanceof ReferenceInstruction r) || !(r.getReference() instanceof MethodReference ref)
                                    || !ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/WacchoiLongPressMenu;"))continue;
                            if(ref.getName().equals("bindSuffixRow")) {
                                if(!m.getName().equals("getView"))throw new AssertionError("Row hook outside adapter");
                                if(m.getImplementation().getRegisterCount()!=5)throw new AssertionError("Row wrapper must keep original argument frame");
                                boolean delegate=false;
                                for(var instruction:m.getImplementation().getInstructions()) {
                                    if(instruction instanceof ReferenceInstruction call && call.getReference() instanceof MethodReference target
                                            && target.getDefiningClass().equals(c.getType()) && target.getName().equals("haiagaruSuffixGetView"))delegate=true;
                                }
                                if(!delegate)throw new AssertionError("Native getView must be delegated without editing its reused parameter registers");
                                rows++;
                            }
                            if(ref.getName().startsWith("suffixCombinedClickable")) {
                                if(!ref.getName().equals(expected))throw new AssertionError("Wrong clickable signature");
                                compose++;
                            }
                        }
                    }
                }
            }
        }
        if(rows<1 || compose!=(args[1].equals("191")?0:1)) throw new AssertionError("rows="+rows+", compose="+compose);
        System.out.println("PASS suffix gesture APK hooks "+args[1]+": rows="+rows+", compose="+compose);
    }
}
