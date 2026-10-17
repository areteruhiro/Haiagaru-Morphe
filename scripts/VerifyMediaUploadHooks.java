import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

/** Verify destination menu + actual upload transport in every patched supported APK. */
public final class VerifyMediaUploadHooks {
    public static void main(String[] args) throws Exception {
        for(String apk:args) {
            Map<String,ClassDef> classes=new HashMap<>();
            var dex=DexFileFactory.loadDexContainer(new File(apk),Opcodes.getDefault());
            for(String entry:dex.getDexEntryNames()) for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses()) classes.put(c.getType(),c);
            Map<String,Integer> hooks=new TreeMap<>();
            for(ClassDef c:classes.values()) for(Method m:c.getMethods()) {
                if(m.getImplementation()==null)continue;
                for(Instruction i:m.getImplementation().getInstructions()) if(i instanceof ReferenceInstruction r
                        && r.getReference() instanceof MethodReference target
                        && target.getDefiningClass().equals("Lapp/morphe/extension/chmate/MediaUploadHosts;")) {
                    hooks.merge(target.getName(),1,Integer::sum);
                    if(i instanceof RegisterRangeInstruction range
                            && range.getStartRegister()+range.getRegisterCount()>m.getImplementation().getRegisterCount())
                        throw new AssertionError("Invalid registers: "+m);
                }
            }
            for(String runtime:List.of("MediaUploadHosts","MediaUploadClient","MediaUploadPolicy","MediaUploadManagement","MediaUploadManagementPolicy","MediaUploadWebActivity","MediaUploadWebPolicy","MediaUploadWebClient","MediaUploadSessions","MediaUploadDeletionClient"))
                if(!classes.containsKey("Lapp/morphe/extension/chmate/"+runtime+";")) throw new AssertionError("Missing "+runtime);
            for(String hook:List.of("init","handles","upload","addPostingMenu","handlePostingMenu","showSettings"))
                if(hooks.getOrDefault(hook,0)<1) throw new AssertionError("Missing "+hook+": "+hooks);
            if(apk.contains("191") && hooks.getOrDefault("handlePostingActivityMenu",0)!=1)
                throw new AssertionError("191 activity-owned menu missing: "+hooks);
            for(String owner:List.of("MediaUploadHosts","NativeUploadHistory")) {
                boolean connected=false;
                for(Method m:classes.get("Lapp/morphe/extension/chmate/"+owner+";").getMethods())if(m.getImplementation()!=null)
                    for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r
                        && r.getReference() instanceof MethodReference ref
                        && ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/MediaUploadManagement;") && ref.getName().equals("show"))connected=true;
                if(!connected)throw new AssertionError("Management menu not connected: "+owner);
            }
            boolean remembers=false,guards=false;
            for(String owner:List.of("MediaUploadHosts","NativeUploadHistory"))
                for(Method m:classes.get("Lapp/morphe/extension/chmate/"+owner+";").getMethods())if(m.getImplementation()!=null)
                    for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r
                        && r.getReference() instanceof MethodReference ref) {
                        if(owner.equals("MediaUploadHosts")&&m.getName().equals("upload")
                            &&ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/MediaUploadManagement;")&&ref.getName().equals("remember"))remembers=true;
                        if(owner.equals("NativeUploadHistory")&&m.getName().equals("delete")
                            &&ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/MediaUploadManagementPolicy;")&&ref.getName().equals("imgur"))guards=true;
                    }
            if(!remembers||!guards)throw new AssertionError("Management persistence or native deletion guard missing");
            boolean webUpload=false, providerDelete=false;
            for(String owner:List.of("MediaUploadHosts","MediaUploadManagement"))
                for(Method m:classes.get("Lapp/morphe/extension/chmate/"+owner+";").getMethods())if(m.getImplementation()!=null)
                    for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r
                        && r.getReference() instanceof MethodReference ref) {
                        if(owner.equals("MediaUploadHosts") && ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/MediaUploadWebClient;") && ref.getName().equals("upload")) webUpload=true;
                        if(owner.equals("MediaUploadManagement") && ref.getDefiningClass().equals("Lapp/morphe/extension/chmate/MediaUploadDeletionClient;") && ref.getName().equals("delete")) providerDelete=true;
                    }
            if(!webUpload||!providerDelete)throw new AssertionError("Web upload / bulk deletion runtime is disconnected");
            System.out.println("PASS "+new File(apk).getName()+": "+hooks);
        }
    }
}
