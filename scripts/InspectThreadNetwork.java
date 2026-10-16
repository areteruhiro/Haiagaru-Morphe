import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.ZipFile;
public final class InspectThreadNetwork {
 public static void main(String[] args)throws Exception {
  var needles=args.length>1?new HashSet<>(Arrays.asList(args).subList(1,args.length)):Set.of("subject.txt","Range","http.keepAlive");
  try(var zip=new ZipFile(args[0])) {
   for(var entry:Collections.list(zip.entries())) {
    if(!entry.getName().matches("classes[0-9]*\\.dex"))continue;
    byte[] data;try(var in=zip.getInputStream(entry)){data=in.readAllBytes();}
    for(var cls:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(data)).getClasses()) {
     for(var method:cls.getMethods()) {
      if(method.getImplementation()==null)continue;
      boolean hit=cls.getInterfaces().stream().anyMatch(needles::contains);
      for(var ins:method.getImplementation().getInstructions()) {
       var ref=ins instanceof ReferenceInstruction r?r.getReference():null;
       if(args.length==1 && ref instanceof MethodReference m && Set.of("setConnectTimeout","setReadTimeout","setMaxRequests","setMaxRequestsPerHost","setMaxIdleConnections").contains(m.getName()))hit=true;
       if(ref instanceof MethodReference m && needles.contains(m.toString()))hit=true;
       if(ref instanceof FieldReference f && needles.contains(f.toString()))hit=true;
       if(ref instanceof StringReference s && needles.contains(s.getString()))hit=true;
      }
      if(!hit)continue;
      System.out.println("METHOD "+method);
      for(var ins:method.getImplementation().getInstructions()) {
       if(ins instanceof ReferenceInstruction r)System.out.println(" "+ins.getOpcode()+" "+r.getReference());
       else if(ins instanceof WideLiteralInstruction l)System.out.println(" "+ins.getOpcode()+" "+l.getWideLiteral());
      }
     }
    }
   }
  }
 }
}
