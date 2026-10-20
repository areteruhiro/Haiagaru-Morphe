import app.morphe.patcher.*;
import app.morphe.patcher.dex.*;
import app.morphe.patcher.patch.BytecodePatchContext;
import app.morphe.patches.chmate.PosterIdChainNgPatchKt;
import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.io.File;
import java.nio.file.*;
import java.util.*;

/** Applies the production hook to each native APK and serializes its changed NG engine to DEX. */
public class VerifyPosterIdChainHooks {
    static final String EXT = "Lapp/morphe/extension/chmate/PosterIdChainNg;";
    static void check(boolean ok, String label) { if (!ok) throw new AssertionError(label); }
    static List<Instruction> code(Method method) {
        List<Instruction> out = new ArrayList<>();
        method.getImplementation().getInstructions().forEach(out::add);
        return out;
    }
    static boolean call(Instruction instruction, String name) {
        return instruction instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m
                && m.getDefiningClass().equals(EXT) && m.getName().equals(name);
    }
    public static void main(String[] args) throws Exception {
        String[][] profiles = {
            {"191 dev","Lo/m9ExternalSyntheticLambda1;","SE_","K","C","Lo/processAdDisplayErrorPostbackForUserError;","h","c","c"},
            {"226 dev","Lo/listener;","d","J","A","Lo/BouncyCastleSocketAdapterCompanion;","j","d","b"},
            {"241","Lo/SignInButton;","a","e","b","Lo/setDislikeWidth;","i","g",""},
            {"242 dev","Lo/zzaW;","c","a","c","Lo/KeJ11;","g","h",""},
            {"243 dev","Lo/zzdpd;","e","b","a","Lo/zzabv;","h","j",""}
        };
        check(args.length == profiles.length, "Provide APKs in 191/226/241/242/243 order");
        for (int p=0; p<profiles.length; p++) {
            String[] profile = profiles[p];
            File temp = Path.of("build/verification/poster-id-chain/hooks-"+p).toAbsolutePath().toFile();
            PatcherConfig config = new PatcherConfig(new File(args[p]),temp,null,null,true,Set.of(),
                    BytecodeMode.FULL,NoOpDexVerifier.INSTANCE);
            config.setBytecodeMode$morphe_patcher(BytecodeMode.FULL);
            try (BytecodePatchContext context = new BytecodePatchContext(config,
                    new PackageMetadata("jp.co.airfront.android.a2chMate","0.8.10."+profile[0],"1",null))) {
                context.decodeDexFiles$morphe_patcher();
                PosterIdChainNgPatchKt.patchPosterIdChainNg(context);
                ClassDef owner = context.mutableClassDefBy(profile[1]);
                Method target = null;
                for (Method method : owner.getMethods()) {
                    if (method.getName().equals(profile[2]) && method.getReturnType().equals("I")) target=method;
                }
                check(target != null,"native response method");
                List<Instruction> instructions=code(target);
                int found=0;
                for (int i=0;i<instructions.size();i++) if (call(instructions.get(i),"merge")) {
                    found++;
                    int p0=target.getImplementation().getRegisterCount()-target.getParameterTypes().size()-1;
                    OneRegisterInstruction result=(OneRegisterInstruction)instructions.get(i+1);
                    boolean modern=profile[8].isEmpty();
                    Instruction store=instructions.get(i+(modern?3:2));
                    int value=store instanceof FiveRegisterInstruction f ? f.getRegisterE()
                            : ((RegisterRangeInstruction)store).getStartRegister()+2;
                    if(modern) {
                        FiveRegisterInstruction call=(FiveRegisterInstruction)instructions.get(i);
                        check(call.getRegisterCount()==3 && call.getRegisterC()==p0
                                && call.getRegisterD()==p0+1 && call.getRegisterE()==value,
                                "owner/response/state passed without clobbering cache");
                        Instruction load=instructions.get(i+2);
                        check(load.getOpcode()==Opcode.IGET && load instanceof TwoRegisterInstruction r
                                && r.getRegisterA()==p0+1 && r.getRegisterB()==p0+1,
                                "merge precedes destructive response-number load");
                    } else {
                        RegisterRangeInstruction call=(RegisterRangeInstruction)instructions.get(i);
                        check(call.getStartRegister()==p0 && call.getRegisterCount()==3,"owner/row/state argument range");
                        TwoRegisterInstruction move=(TwoRegisterInstruction)instructions.get(i-1);
                        check(move.getRegisterA()==p0+2 && move.getRegisterB()==value,"legacy scratch argument");
                    }
                    check(result.getRegisterA()==value,"native chain-state register preserved");
                }
                check(found==1,"one merge hook");
                if (!profile[8].isEmpty()) {
                    int resets=0;
                    for (Method m:owner.getMethods()) if(m.getName().equals(profile[8]) && m.getReturnType().equals("V")
                            && m.getParameterTypes().equals(List.of("Z")))
                        for(Instruction i:code(m))if(call(i,"reset"))resets++;
                    check(resets==1,"legacy rebuild resets snapshots");
                }
                String matcher=null;
                boolean rows=false;
                for(Field field:owner.getInstanceFields()) {
                    if(field.getName().equals(profile[3]))rows=true;
                    if(field.getName().equals(profile[4]))matcher=field.getType();
                }
                check(rows && matcher!=null,"native source list and WORD matcher");
                boolean items=false;
                for(String type=matcher; type!=null && !type.equals("Ljava/lang/Object;" );) {
                    ClassDef cls=context.classDefBy(type);
                    for(Field field:cls.getInstanceFields()) if(field.getType().equals("Ljava/util/List;"))items=true;
                    type=cls.getSuperclass();
                }
                check(items,"board-filtered native rule list available");
                for(String name:List.of(profile[6],profile[7])) {
                    boolean fieldFound=false;
                    for(Field field:context.classDefBy(profile[5]).getInstanceFields())
                        if(field.getName().equals(name)&&field.getType().equals("Ljava/lang/String;"))fieldFound=true;
                    check(fieldFound,"native ID/body field "+name);
                }
                Path dex=temp.toPath().resolve("ng-engine.dex"); Files.createDirectories(dex.getParent());
                DexPool.writeTo(dex.toString(),new ImmutableDexFile(Opcodes.getDefault(),List.of(owner)));
                check(Files.size(dex)>0,"patched engine serialized");
                System.out.println("PASS "+profile[0]+" production hook, argument registers, native model/rule layout and DEX serialization");
            }
            System.gc();
        }
    }
}
