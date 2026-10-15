import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.Opcode;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.instruction.*;
import com.android.tools.smali.dexlib2.iface.reference.*;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.ZipFile;

/** Offline regression check; never logs credentials or sends requests. */
public final class VerifyUplift241 {
    public static void main(String[] args) throws Exception {
        int token = 0, login = 0, callers = 0, client = 0, diagnostic = 0, ttl = 0;
        try (var zip = new ZipFile(args[0])) {
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                byte[] bytes;
                try (var in = zip.getInputStream(entry)) { bytes = in.readAllBytes(); }
                for (var cls : new DexBackedDexFile(Opcodes.getDefault(), ByteBuffer.wrap(bytes)).getClasses()) {
                    if (!cls.getType().equals("Lo/VLj;")) continue;
                    for (var method : cls.getMethods()) {
                        if (method.getImplementation() == null) continue;
                        var ins = new ArrayList<Instruction>();
                        method.getImplementation().getInstructions().forEach(ins::add);
                        var params = method.getParameterTypes().stream().map(Object::toString).toList();
                        boolean isToken = method.getName().equals("c") && params.equals(List.of("Lo/VLj$ComponentActivity;"));
                        boolean isLogin = method.getName().equals("c") && params.equals(List.of("Lo/VLj$ComponentActivity;", "Lo/getPaint;", "J"));
                        if (isToken) token++;
                        if (isLogin) login++;
                        for (int i = 0; i < ins.size(); i++) {
                            var instruction = ins.get(i);
                            if ((isToken || isLogin) && instruction.getOpcode() == Opcode.IF_NE) {
                                long agets = ins.subList(Math.max(0, i - 8), i).stream()
                                    .filter(x -> x.getOpcode() == Opcode.AGET).count();
                                if (agets >= 2) throw new AssertionError("Certificate-dependent UPLIFT guard remains: " + params);
                            }
                            if (isToken && instruction instanceof NarrowLiteralInstruction literal && literal.getNarrowLiteral() == 1284195) {
                                var window = ins.subList(i + 1, Math.min(i + 4, ins.size()));
                                if (window.stream().anyMatch(x -> x.getOpcode() == Opcode.DIV_INT || x.getOpcode() == Opcode.DIV_INT_2ADDR))
                                    throw new AssertionError("Token expiry division remains");
                                if (window.stream().noneMatch(x -> x instanceof NarrowLiteralInstruction n && n.getNarrowLiteral() == 0))
                                    throw new AssertionError("Token expiry fallback missing");
                                ttl++;
                            }
                            if (!(instruction instanceof ReferenceInstruction ref)) continue;
                            if (isLogin && ref.getReference() instanceof StringReference s && s.getString().startsWith("2,null cannot be cast")) {
                                var prior = ins.get(i - 1);
                                if (!(prior instanceof NarrowLiteralInstruction n) || n.getNarrowLiteral() != 2)
                                    throw new AssertionError("Diagnostic calculation still executes");
                                diagnostic++;
                            }
                            if (ref.getReference() instanceof MethodReference call) {
                                if (call.getDefiningClass().equals("Lo/VLj;") && call.getName().equals("c")
                                    && call.getParameterTypes().stream().map(Object::toString).toList().equals(List.of("Lo/VLj$ComponentActivity;"))) callers++;
                                if (isLogin && call.getDefiningClass().equals("Lo/setDownloadListener;") && call.getName().equals("c")) client++;
                            }
                        }
                    }
                }
            }
        }
        if (token != 1 || login != 1 || callers < 2 || client != 1 || diagnostic != 1 || ttl != 1)
            throw new AssertionError("Missing paths: " + List.of(token, login, callers, client, diagnostic, ttl));
        System.out.println("PASS UPLIFT241: token/login guards, expiry, diagnostic, real login and shared posting paths");
    }
}
