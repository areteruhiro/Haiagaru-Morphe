import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import java.util.*;
import java.util.zip.ZipFile;
public final class InspectUploadDexClass {
 public static void main(String[] args)throws Exception{
  try(var zip=new ZipFile(args[0])){
   for(var e:Collections.list(zip.entries())){
    if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
    byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
    for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses()){
     if(Arrays.stream(args).skip(1).noneMatch(s->s.endsWith("*")?c.getType().startsWith(s.substring(0,s.length()-1)):c.getType().equals(s)))continue;
     System.out.println("CLASS "+c.getType()+" extends="+c.getSuperclass()+" flags="+c.getAccessFlags());c.getFields().forEach(f->System.out.println("FIELD "+f+" flags="+f.getAccessFlags()));
     for(var m:c.getMethods()){
      System.out.println("METHOD "+m);
      if(m.getImplementation()==null)continue;
      int n=0;for(var i:m.getImplementation().getInstructions()){
       String d=i instanceof ReferenceInstruction r?" "+r.getReference():i instanceof WideLiteralInstruction l?" "+l.getWideLiteral():"";
       String regs=i instanceof FiveRegisterInstruction r?" {"+r.getRegisterC()+","+r.getRegisterD()+","+r.getRegisterE()+","+r.getRegisterF()+","+r.getRegisterG()+"} n="+r.getRegisterCount():i instanceof RegisterRangeInstruction r?" {"+r.getStartRegister()+"..} n="+r.getRegisterCount():i instanceof TwoRegisterInstruction r?" {"+r.getRegisterA()+","+r.getRegisterB()+"}":i instanceof OneRegisterInstruction r?" {"+r.getRegisterA()+"}":"";
       if(i instanceof OffsetInstruction o)d+=" offset="+o.getCodeOffset();
       if(i instanceof SwitchPayload s)d+=" cases="+s.getSwitchElements().stream().map(x->x.getKey()+":"+x.getOffset()).toList();
       System.out.println((n++)+" "+i.getOpcode()+d+regs);
      }
     }
    }
   }
  }
 }
}
