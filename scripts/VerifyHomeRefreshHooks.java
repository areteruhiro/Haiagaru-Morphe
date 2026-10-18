import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.io.File;
import java.util.*;

public final class VerifyHomeRefreshHooks {
 public static void main(String[] args)throws Exception{
  Map<String,ClassDef> classes=new HashMap<>();var dex=DexFileFactory.loadDexContainer(new File(args[0]),Opcodes.getDefault());
  for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses())classes.put(c.getType(),c);
  String helper="Lapp/morphe/extension/chmate/HomeRefreshKeywords;";
  for(String owner:new String[]{"Ljp/syoboi/a2chMate/ui/home/HomeViewModel$1$5;","Ljp/syoboi/a2chMate/ui/home/HomeViewModel$4$3;"}){
   int found=0;for(Method m:classes.get(owner).getMethods()){if(!m.getName().equals("emit"))continue;
    List<Instruction> body=new ArrayList<>();m.getImplementation().getInstructions().forEach(body::add);
    for(int i=0;i<body.size();i++)if(body.get(i) instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference ref&&ref.getDefiningClass().equals(helper)&&ref.getName().equals("refresh")){
     if(!(body.get(i) instanceof RegisterRangeInstruction source)||source.getStartRegister()!=0||source.getRegisterCount()!=1)throw new AssertionError("wrong sorter argument");
     if(body.get(i+1).getOpcode()!=Opcode.MOVE_RESULT_OBJECT||((OneRegisterInstruction)body.get(i+1)).getRegisterA()!=6)throw new AssertionError("wrong keyword argument");
     if(!(body.get(i+2) instanceof ReferenceInstruction next)||!(next.getReference() instanceof MethodReference target)||!target.getDefiningClass().equals("Lo/ModuleInstallRequestBuilder;")||!target.getName().equals("d"))throw new AssertionError("missing native sort after keyword restore");found++;
    }
   }if(found!=1)throw new AssertionError("refresh hook count "+found+" "+owner);
  }
  int remember=0;for(Method m:classes.get("Lo/ModuleInstallRequestBuilder;").getMethods())if(m.getName().equals("d")&&m.getParameters().size()==6)
   for(Instruction i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference ref&&ref.getDefiningClass().equals(helper)&&ref.getName().equals("remember"))remember++;
  if(remember!=1)throw new AssertionError("normal keyword capture "+remember);
  System.out.println("PASS: both native refresh callers restore keywords; normal sorter records settings changes");
 }
}
