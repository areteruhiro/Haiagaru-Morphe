import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.util.*;
import java.util.zip.ZipFile;

/** Verifies theme tint on actual final APKs, without relying on decompiled source. */
public final class VerifyAutoScrollColors {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        for (String apk : args) try (var zip = new ZipFile(apk)) {
            Map<String, ClassDef> classes = new HashMap<>();
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes; try (var input = zip.getInputStream(entry)) { bytes = input.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), java.nio.ByteBuffer.wrap(bytes)).getClasses()) classes.put(cls.getType(), cls);
            }
            String[] profile = null;
            for (String[] candidate : new String[][]{{"Lo/getChannel;", "Lo/addConnectException;", "bv"},
                    {"Lo/QWA11;", "Lo/getPageMargin;", "bs"}, {"Lo/AdChoicesView;", "Lo/onTextChanged;", "bq"}}) {
                ClassDef owner=classes.get(candidate[0]); if(owner==null)continue;
                Set<String> labels=new HashSet<>();
                for(var method:owner.getMethods())if(method.getImplementation()!=null)
                    for(var ins:method.getImplementation().getInstructions())
                        if(ins instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s)labels.add(s.getString());
                if(labels.containsAll(Set.of("Up","Down"))){profile=candidate;break;}
            }
            check(profile!=null,"Native auto-scroll profile missing");
            final String[] selectedProfile=profile;
            ClassDef icons = classes.get(profile[0]); check(icons != null, "Native icons missing: " + apk);
            int verified = 0;
            for (var method : icons.getMethods()) {
                if (method.getImplementation() == null) continue;
                List<Instruction> body = new ArrayList<>(); method.getImplementation().getInstructions().forEach(body::add);
                if (body.stream().noneMatch(ins -> ins instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s
                        && Set.of("Up", "Down").contains(s.getString()))) continue;
                check(body.stream().anyMatch(ins -> ins instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m
                        && m.getDefiningClass().equals("Lapp/morphe/extension/chmate/Haiagaru;") && m.getName().equals("applicationContextForExtension")), "Missing application context");
                check(body.stream().anyMatch(ins -> ins instanceof ReferenceInstruction r && r.getReference() instanceof FieldReference f
                        && f.getDefiningClass().equals(selectedProfile[1]) && f.getName().equals(selectedProfile[2])), "Missing native primary text color");
                int index = -1;
                for (int i=0;i<body.size();i++) if (body.get(i) instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m
                        && m.getParameterTypes().size()==7 && m.getParameterTypes().get(1).equals("Ljava/lang/String;")
                        && m.getParameterTypes().get(3).equals("J") && m.getReturnType().equals("V")) index=i;
                check(index >= 9, "Missing icon call");
                var call=(RegisterRangeInstruction)body.get(index);
                check(body.get(index-1) instanceof NarrowLiteralInstruction n && n.getNarrowLiteral()==4
                        && ((OneRegisterInstruction)n).getRegisterA()==call.getStartRegister()+7, "Tint default not disabled");
                check(body.get(index-2).getOpcode().name().equals("SHL_LONG_2ADDR"), "Missing sRGB packing");
                check(body.get(index-3) instanceof NarrowLiteralInstruction n && n.getNarrowLiteral()==32, "Incorrect sRGB shift");
                verified++;
            }
            check(verified==2,"Expected two native auto-scroll icons");
            System.out.println("PASS " + apk + ": 2 themed native icons; explicit tint and sRGB packing");
        }
    }
}
