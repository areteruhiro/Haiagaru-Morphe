import com.android.tools.smali.dexlib2.*;
import com.android.tools.smali.dexlib2.dexbacked.*;
import com.android.tools.smali.dexlib2.iface.*;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.*;
public final class VerifyDirectSelectionHooks {
    static final String TARGET="Lapp/morphe/extension/chmate/DirectBodySelection;";
    static Map<String,ClassDef> classes=new HashMap<>();
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    static boolean calls(String owner,String name,String target,String method){
        for(Method m:classes.get(owner).getMethods())if(name.equals(m.getName())&&m.getImplementation()!=null)
            for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r&&r.getReference() instanceof MethodReference ref
                    &&ref.getDefiningClass().equals(target)&&ref.getName().equals(method))return true;
        return false;
    }
    static boolean string(String owner,String name,String value){
        for(Method m:classes.get(owner).getMethods())if(name.equals(m.getName())&&m.getImplementation()!=null)
            for(var i:m.getImplementation().getInstructions())if(i instanceof ReferenceInstruction r&&r.getReference() instanceof StringReference ref&&ref.getString().equals(value))return true;
        return false;
    }
    public static void main(String[] args)throws Exception {
        try(var zip=new ZipFile(args[0])){for(var e:Collections.list(zip.entries())){
            if(!e.getName().matches("classes[0-9]*\\.dex"))continue;
            byte[] bytes;try(var in=zip.getInputStream(e)){bytes=in.readAllBytes();}
            for(var c:new DexBackedDexFile(Opcodes.getDefault(),java.nio.ByteBuffer.wrap(bytes)).getClasses())classes.put(c.getType(),c);
        }}
        String owner=switch(args[1]){case "191"->"Lo/r8lambda1VMNzQcdpAFQ0ukC0aRPi4Owdo;";case "226"->"Lo/PAGClientBidding;";case "242"->"Lo/zzbhr;";default->throw new AssertionError();};
        check(calls(owner,"dispatchTouchEvent",TARGET,"prepare"),"Opt-in gate connected");
        check(calls(owner,"setOnLongClickListener",TARGET,"longClickListener"),"Native listener gate connected");
        check(calls(owner,"showContextMenu",TARGET,"prepare"),"Context menu suppressed only on opted-in bodies");
        check(calls(TARGET,"dispatch","Landroid/view/ViewParent;","requestDisallowInterceptTouchEvent"),"Selection gesture protected from parent cancellation");
        long contextOverloads=0;
        for(Method m:classes.get(owner).getMethods())if(m.getName().equals("showContextMenu"))contextOverloads++;
        check(contextOverloads==2,"Both context menu overloads guarded");
        check(calls(owner,"dispatchTouchEvent",TARGET,"dispatch"),"Direct selection connected");
        check(calls(owner,"dispatchTouchEvent",owner,"haiagaruDirectSelectionNativeDispatch"),"Native off path retained");
        check(calls(TARGET,"prepare","Landroid/widget/TextView;","setCustomSelectionActionModeCallback"),"Platform selection callback switch and restoration connected");
        String haiagaru="Lapp/morphe/extension/chmate/Haiagaru;";
        check(string(haiagaru,"addQuickFilterToolbarChoice","haiagaru_direct_copy"),"Response toolbar catalog contains long-press copy");
        check(string(haiagaru,"handleEdgeArchiveToolbarClick","directBodySelection"),"Toolbar toggles shared direct selection preference");
        check(calls(haiagaru,"handleEdgeArchiveToolbarClick","Lapp/morphe/extension/chmate/EmojiFontFallback;","refreshVisibleRows"),"Toolbar toggles refresh visible response bodies");
        check(calls(owner,"haiagaruDirectSelectionNativeDispatch","Lapp/morphe/extension/chmate/XPostPreviewTouch;","onTouch"),"Existing X-preview dispatch retained");
        check(calls("Lapp/morphe/extension/chmate/XPostPreview;","onBound",TARGET,"prepare"),"Recycled body gate connected");
        check(calls("Lapp/morphe/extension/chmate/EmojiFontFallback;","applyToRow",TARGET,"prepare"),"Setting refresh connected");
        if(args[1].equals("191"))check(calls(owner,"onSelectionChanged",owner,"haiagaruNativeSelectionChanged"),"191 native selection fallback retained");
        System.out.println("PASS "+args[1]+" native direct selection DEX hooks and original dispatch");
    }
}
