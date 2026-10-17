import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.*;
public final class VerifyHiddenIdHooks {
    static final String NG="Lapp/morphe/extension/chmate/HiddenIdNg;";
    static final String PROGRAM="Lapp/morphe/extension/chmate/ProgrammableNgController;";
    static final Map<String,ClassDef> classes=new HashMap<>();
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    static boolean calls(String owner,String method,String target,String name){
        for(Method m:classes.get(owner).getMethods())if(m.getName().equals(method)&&m.getImplementation()!=null)
            for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference ref
                    &&ref.getDefiningClass().equals(target)&&ref.getName().equals(name))return true;
        return false;
    }
    public static void main(String[] args)throws Exception{
        try(var zip=new ZipFile(args[0])){for(var e:Collections.list(zip.entries())){
            if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
            try(var in=zip.getInputStream(e)){
                for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(in.readAllBytes())).getClasses())classes.put(c.getType(),c);
            }
        }}
        check(calls("Lapp/morphe/extension/chmate/Haiagaru;","initializeApplicationContext",NG,"initialize"),"ID NG initialized");
        if(args[1].equals("191")){
            check(calls("Lo/m9ExternalSyntheticLambda1;","c",PROGRAM,"prepareLegacyResponses"),"191 rebuild connected");
            check(calls(PROGRAM,"prepareLegacyResponses",NG,"prepare"),"191 source indexed");
            check(calls("Lo/m9ExternalSyntheticLambda1;","SE_",PROGRAM,"mergeLegacyResponseFlags"),"191 flags connected");
            check(calls(PROGRAM,"mergeLegacyResponseFlags",NG,"merge"),"191 built-in NG independent of scripts");
        }else if(args[1].equals("226")){
            check(calls("Lo/listener;","b",NG,"prepare"),"226 rebuild connected");
            check(calls("Lo/listener;","d",PROGRAM,"mergeModernResponseFlags"),"226 flags connected");
            check(calls(PROGRAM,"mergeModernResponseFlags",NG,"merge"),"226 built-in NG independent of scripts");
        }else{
            check(calls("Lo/AvailabilityException;","<init>",NG,"bind242"),"242 exact thread origin");
            check(calls(PROGRAM,"pendingModernResponses",NG,"pending"),"242 response list connected");
            int hooks=0;
            for(Method m:classes.get("Lo/emptyToNull;").getMethods())if(m.getImplementation()!=null)
                for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference ref
                        &&ref.getDefiningClass().equals(NG)&&ref.getName().equals("modernFlags"))hooks++;
            check(hooks==2,"242 full and compact typed map overlays");
        }
        System.out.println("PASS "+args[1]+" hidden-ID DEX hook checks");
    }
}
