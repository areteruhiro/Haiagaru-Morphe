import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.android.tools.smali.dexlib2.iface.reference.MethodReference;
import java.util.*;
import java.util.zip.ZipFile;
public class InspectWacchoiMenuViews {
 public static void main(String[] args) throws Exception {
  try(var zip=new ZipFile(args[0])) { for(var e:Collections.list(zip.entries())) {
   if(!e.getName().matches("classes[0-9]*\\.dex")) continue;
   byte[] b; try(var in=zip.getInputStream(e)){b=in.readAllBytes();}
   for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(b)).getClasses()) {
    boolean target=Arrays.stream(args).skip(1).anyMatch(s->c.getType().startsWith(s));
    for(var m:c.getMethods()) {
     if(m.getImplementation()==null) continue;
     if(Arrays.asList(args).contains("--clicks")) {
      var p=m.getParameterTypes();
      if(p.size()>=6 && m.getReturnType().equals(p.get(0).toString()) && p.stream().filter(x->x.toString().equals("Ljava/lang/String;")).count()>=2
         && p.stream().filter(x->x.toString().equals("Z")).count()>=1) System.out.println(m);
      continue;
     }
     var refs=new ArrayList<String>();
     for(var i:m.getImplementation().getInstructions()) if(i instanceof ReferenceInstruction r) refs.add(r.getReference().toString());
     boolean popup=m.getName().equals("onItemClick") && refs.stream().anyMatch(s->s.contains("getSubMenu")) && refs.stream().anyMatch(s->s.contains("getItemAtPosition"));
     boolean usage=Arrays.stream(args).skip(1).anyMatch(s->refs.stream().anyMatch(r->r.contains(s)));
     if(target||(!Arrays.asList(args).contains("--only") && (popup||usage))) {
      System.out.println("METHOD " + m);
      if(Arrays.asList(args).contains("--code")) for(var ins:m.getImplementation().getInstructions()) {
       String regs="";
       if(ins instanceof com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction r) regs=" "+r.getRegisterC()+","+r.getRegisterD()+","+r.getRegisterE()+","+r.getRegisterF()+","+r.getRegisterG();
       else if(ins instanceof com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction r) regs=" "+r.getStartRegister()+".."+(r.getStartRegister()+r.getRegisterCount()-1);
       else if(ins instanceof com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction r) regs=" "+r.getRegisterA()+","+r.getRegisterB();
       else if(ins instanceof com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction r) regs=" "+r.getRegisterA();
       System.out.println(ins.getOpcode()+regs+(ins instanceof com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction l?" #"+l.getNarrowLiteral():"")+(ins instanceof ReferenceInstruction r?" "+r.getReference():""));
      } else refs.forEach(System.out::println);
     }
    }
   }
  }}
 }
}
