import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.*;
public final class VerifyPosterHighlightHooks {
    static final String TARGET="Lapp/morphe/extension/chmate/PosterHighlight;";
    static Map<String,ClassDef> classes=new HashMap<>();
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    static boolean calls(String owner,String name,String target,String method){
        for(Method m:classes.get(owner).getMethods())if((name==null||name.equals(m.getName()))&&m.getImplementation()!=null)
            for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference ref
                    &&ref.getDefiningClass().equals(target)&&ref.getName().equals(method))return true;
        return false;
    }
    public static void main(String[] args)throws Exception{
        try(var zip=new ZipFile(args[0])){for(var e:Collections.list(zip.entries())){
            if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
            byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
            for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses())classes.put(c.getType(),c);
        }}
        String owner,name;
        switch(args[1]){
            case "191":owner="Lo/m9ExternalSyntheticLambda1;";name="SC_";break;
            case "226":owner="Lo/listener;";name="c";break;
            case "242":owner="Lo/WorkSourceUtil;";name="d";break;
            default:throw new AssertionError("Unsupported profile");
        }
        check(calls(owner,name,TARGET,"decorate"),"Renderer connected to highlighting");
        check(calls(owner,name,owner,"haiagaruRenderPoster"),"Native renderer retained");
        check(calls("Lapp/morphe/extension/chmate/ResponseArchive;","append",TARGET,"append"),"Legacy exact-target menu connected");
        check(calls("Lapp/morphe/extension/chmate/ResponseArchive;","handle",TARGET,"handle"),"Native popup dispatch connected");
        check(calls("Lapp/morphe/extension/chmate/WacchoiLongPressMenu;","dispatchSuffixNgItem","Lapp/morphe/extension/chmate/ResponseArchive;","handle"),"Legacy popup reaches highlight dispatcher");
        check(calls("Lapp/morphe/extension/chmate/ResponseArchive;","appendSheet",TARGET,"appendSheet"),"Sheet exact-target menu connected");
        if(args[1].equals("242")){
            check(calls("Ljp/syoboi/a2chMate/fragment/ResMenuDialogFragment;","c","Lapp/morphe/extension/chmate/ResponseArchive;","handle"),"Compose click reaches highlight dispatcher");
            check(calls("Ljp/syoboi/a2chMate/fragment/ResMenuDialogFragment;","g","Lapp/morphe/extension/chmate/ResponseArchive;","appendDialog242"),"Ordinary 242 menu connected");
            check(calls("Lapp/morphe/extension/chmate/ProgrammableNgController;","pendingModernResponses",TARGET,"observeResponses"),"242 starter-list boundary connected");
        }
        System.out.println("PASS "+args[1]+" native poster-renderer/menu DEX bindings");
    }
}
