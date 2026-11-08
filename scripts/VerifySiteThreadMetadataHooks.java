import java.util.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;

/** Offline comparison against the preceding verified APK, with only metadata hooks removed. */
public final class VerifySiteThreadMetadataHooks {
    static void ok(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception {
        String version=args[0],owner=version.equals("191")?"Lo/m8a;":version.equals("226")?"Lo/getWriteBytesTotal;":"Lo/zzavb;";
        var before=VerifySearchHistoryHooks.classes(args[1]);var after=VerifySearchHistoryHooks.classes(args[2]);
        int epochs=0,momentum=0,scopes=0;
        for(Method original:before.get(owner).getMethods()){
            if(original.getImplementation()==null)continue;
            Method patched=null;for(Method candidate:after.get(owner).getMethods())if(candidate.getName().equals(original.getName())&&candidate.getParameterTypes().equals(original.getParameterTypes())&&candidate.getReturnType().equals(original.getReturnType()))patched=candidate;
            ok(patched!=null,"method retained");ok(original.getImplementation().getRegisterCount()==patched.getImplementation().getRegisterCount(),"register count retained");
            var expected=VerifySearchHistoryHooks.code(original);var actual=VerifySearchHistoryHooks.code(patched);int position=0;
            for(int n=0;n<actual.size();n++){
                Instruction ins=actual.get(n);MethodReference ref=ins instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference m?m:null;
                if(ref!=null&&ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/SiteThreadMetadata;")){
                    if(ref.getName().equals("epoch")||ref.getName().equals("momentum")){
                        ok(n>0&&actual.get(n-1).getOpcode().name().equals("IGET_WIDE"),"hook uses the exact native long field read");
                        int pair=((OneRegisterInstruction)actual.get(n-1)).getRegisterA();
                        ok(ins instanceof RegisterRangeInstruction range&&range.getStartRegister()==pair&&range.getRegisterCount()==2,"wide invoke register pair");
                        ok(n+1<actual.size()&&actual.get(n+1).getOpcode().name().equals("MOVE_RESULT_WIDE")&&((OneRegisterInstruction)actual.get(n+1)).getRegisterA()==pair,"wide result register pair");
                        ok(pair+1<patched.getImplementation().getRegisterCount(),"no register overflow");n++;
                        if(ref.getName().equals("epoch"))epochs++;else momentum++;
                    }else {ok(Set.of("begin","end").contains(ref.getName()),"only known scope hooks");scopes++;}
                    continue;
                }
                ok(position<expected.size()&&VerifySearchHistoryHooks.key(expected.get(position++)).equals(VerifySearchHistoryHooks.key(ins)),"native instruction retained: "+original+" at "+n);
            }
            ok(position==expected.size(),"every baseline instruction retained");
        }
        ok(epochs==(version.equals("242")?2:4),"expected fixed epoch anchors");
        ok(momentum==(version.equals("242")?3:0),"expected fixed momentum anchors");
        ok(version.equals("242")?scopes==0:scopes>=2,"legacy calculation scopes");
        System.out.println("PASS "+version+": metadata hooks, wide registers, every native instruction and stable ID field retained");
    }
}
