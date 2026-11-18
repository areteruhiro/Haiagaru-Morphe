import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.io.*;import java.util.*;
public class VerifyResponseOrderPersistence {
 public static void main(String[] a)throws Exception {
  String owner=a[1].equals("191")?"Lo/pa;":a[1].equals("226")?"Ljp/syoboi/a2chMate/ui/reslist/ResListFragment;":"Lo/zzabe;";
  String name=a[1].equals("191")?"b":a[1].equals("226")?"f":"c";
  var dex=DexFileFactory.loadDexContainer(new File(a[0]),Opcodes.getDefault());boolean found=false;
  for(String entry:dex.getDexEntryNames())for(ClassDef c:dex.getEntry(entry).getDexFile().getClasses())if(c.getType().equals(owner))for(Method m:c.getMethods())if(m.getName().equals(name)&&m.getReturnType().equals("V")&&m.getImplementation()!=null){
   for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference call){
    if(a[1].equals("242")&&call.getDefiningClass().equals("Lapp/morphe/extension/chmate/NativeResponseOrder;")&&call.getName().equals("save242"))found=true;
    if(!a[1].equals("242")&&call.getDefiningClass().equals("Ljp/syoboi/a2chMate/Prefs;")&&call.getParameterTypes().size()==2&&call.getParameterTypes().get(1).equals("I")&&call.getReturnType().equals("V")){
     var invoke=(FiveRegisterInstruction)i;
     int param=m.getImplementation().getRegisterCount()-1;
     if(invoke.getRegisterD()!=param)throw new AssertionError("Wrong selected order register");found=true;
    }
   }
  }
  if(!found)throw new AssertionError("Immediate order save missing "+a[1]);System.out.println("PASS "+a[1]+" immediate native response-order persistence");
 }
}
