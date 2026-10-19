import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

/** Inspect actual final APKs for batch preparation and typed, non-mutating map replacement hooks. */
public class VerifyProgrammableNgHooks {
    static final String API="Lapp/morphe/extension/chmate/ProgrammableNgController;";
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] paths)throws Exception {
        for(String path:paths)try(var zip=new ZipFile(path)) {
            int legacy=0,modern=0,merges=0,overlays=0,bindings=0;
            for(var entry:Collections.list(zip.entries())) {
                if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;
                for(var cls:new DexBackedDexFile(null,ByteBuffer.wrap(zip.getInputStream(entry).readAllBytes())).getClasses())
                    for(var method:cls.getMethods()) {
                        if(!cls.getType().startsWith("Lo/")||method.getImplementation()==null)continue;
                        var instructions=new ArrayList<Instruction>();method.getImplementation().getInstructions().forEach(instructions::add);
                        for(int i=0;i<instructions.size();i++) {
                            var inst=instructions.get(i);
                            if(!(inst instanceof ReferenceInstruction ref)||!(ref.getReference() instanceof MethodReference m)||!m.getDefiningClass().equals(API))continue;
                            switch(m.getName()) {
                                case "prepareLegacyResponses" -> {check(method.getName().equals("c"),"legacy batch rebuild");legacy++;}
                                case "prepareModernResponses" -> {check(method.getName().equals("b"),"226 batch rebuild");modern++;}
                                case "mergeLegacyResponseFlags", "mergeModernResponseFlags" -> merges++;
                                case "bindModernResponses" -> {check(method.getName().equals("<init>"),"exact board binding constructor");bindings++;}
                                case "applyPendingModernResponseFlags" -> {
                                    check(m.getReturnType().equals("Ljava/util/Map;"),"overlay must return a typed Map");
                                    check(instructions.get(i+1).getOpcode().name().equals("MOVE_RESULT_OBJECT"),"overlay result must replace native argument");
                                    int argument=((RegisterRangeInstruction)inst).getStartRegister();
                                    check(((OneRegisterInstruction)instructions.get(i+1)).getRegisterA()==argument,"overlay must replace the same map register");
                                    overlays++;
                                }
                            }
                        }
                    }
            }
            if(legacy>0)check(legacy==1&&merges>=2&&modern==0&&overlays==0,"191 hooks");
            else if(modern>0)check(modern==1&&merges>=2&&overlays==0,"226 hooks");
            else check(overlays==2&&bindings==1,"242 full/compact overlays and exact board binding");
            System.out.println("PASS batch NG preparation, typed overlay and native argument hooks: "+path);
        }
    }
}
