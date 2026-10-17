import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

/** Checks the real patched APK, including native reflection field layouts. */
public final class VerifyEdgeReporterDisplayHooks {
    public static void main(String[] args) throws Exception {
        String runtime = "Lapp/morphe/extension/chmate/EdgeReporterDisplay;";
        for (String apk : args) {
            Map<String,ClassDef> classes = new HashMap<>();
            var dex = DexFileFactory.loadDexContainer(new File(apk),Opcodes.getDefault());
            for (String entry : dex.getDexEntryNames())
                for (ClassDef cls : dex.getEntry(entry).getDexFile().getClasses()) classes.put(cls.getType(),cls);
            String[] layout = apk.contains("191")
                    ? new String[]{"Lo/m8a;","r","Lo/MaxAdViewImplExternalSyntheticLambda1;","e","getView"}
                    : apk.contains("226")
                    ? new String[]{"Lo/getWriteBytesTotal;","g","Lo/CertificateChainCleaner;","b","getView"}
                    : new String[]{"Lo/zzavb;","c","Lo/setSkipText;","d","onBindViewHolder"};
            for (int i : new int[]{0,2}) {
                boolean found = false;
                for (Field field : classes.get(layout[i]).getFields())
                    if (field.getName().equals(layout[i+1]) && (i == 0
                            ? field.getType().equals("Ljp/syoboi/a2chMate/client/BBSUrlInfo;")
                            : field.getType().equals("J"))) found = true;
                if (!found) throw new AssertionError("Native field changed: " + Arrays.toString(layout));
            }
            Method bind = null;
            for (Method method : classes.get(layout[0]).getMethods())
                if (method.getName().equals(layout[4])) bind = method;
            List<Instruction> instructions = new ArrayList<>();
            bind.getImplementation().getInstructions().forEach(instructions::add);
            Map<String,Integer> calls = new TreeMap<>();
            int returns = 0;
            for (int i=0; i<instructions.size(); i++) {
                Instruction instruction = instructions.get(i);
                if (instruction instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m
                        && m.getDefiningClass().equals(runtime)) {
                    calls.merge(m.getName(),1,Integer::sum);
                    if (instruction instanceof RegisterRangeInstruction range
                            && range.getStartRegister()+range.getRegisterCount()>bind.getImplementation().getRegisterCount())
                        throw new AssertionError("Register overflow: " + bind);
                }
                if (instruction.getOpcode() == Opcode.RETURN_OBJECT || instruction.getOpcode() == Opcode.RETURN_VOID) {
                    returns++;
                    if (!(instructions.get(i-1) instanceof ReferenceInstruction r)
                            || !(r.getReference() instanceof MethodReference m)
                            || !m.getDefiningClass().equals(runtime) || !m.getName().equals("end"))
                        throw new AssertionError("Uncleared display state: " + bind);
                }
            }
            for (String name : new String[]{"begin","subject","title"})
                if (calls.getOrDefault(name,0)!=1) throw new AssertionError("Missing hook: " + calls);
            if(calls.getOrDefault("end",0)!=returns) throw new AssertionError(calls);
            System.out.println("PASS " + new File(apk).getName() + ": native fields + display hooks " + calls);
        }
    }
}
