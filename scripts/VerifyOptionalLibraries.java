import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;

/** Run against compiled patches classes and the Morphe CLI dependency classpath. */
public class VerifyOptionalLibraries {
    static Map<String,String> hashes(Path file, String library) throws Exception {
        var result = new LinkedHashMap<String,String>();
        try (var zip = new ZipFile(file.toFile())) {
            for (var entry : Collections.list(zip.entries())) {
                if (!entry.getName().endsWith("/" + library)) continue;
                String[] parts = entry.getName().split("/");
                try (var in = zip.getInputStream(entry)) {
                    result.put(parts[parts.length - 2], HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(in.readAllBytes())));
                }
            }
        }
        return result;
    }
    public static void main(String[] args) throws Exception {
        var method = Class.forName("app.morphe.patches.chmate.OptionalAssetsKt").getDeclaredMethod("optionalLibraries", String.class, Map.class, String.class);
        method.setAccessible(true);
        var aar = Path.of("vpn/image-stack/prebuilt/image-stack.aar");
        var vpn = Path.of("build/optional-assets/haiagaru-image-vpn.zip");
        var image = Path.of("optional-assets/haiagaru-image-ng.zip");
        var vpnHashes = hashes(aar, "libgojni.so");
        var imageHashes = hashes(image, "libtensorflowlite_jni.so");
        if (vpnHashes.size() != 4 || imageHashes.size() != 4 || !hashes(vpn, "libgojni.so").equals(vpnHashes)) throw new AssertionError("ABI / packaged hashes");
        if (args.length > 0) {
            var apk = Path.of(args[0]);
            if (!hashes(apk, "libgojni.so").equals(vpnHashes) || !hashes(apk, "libtensorflowlite_jni.so").equals(imageHashes))
                throw new AssertionError("Patched APK native assets differ");
            try (var zip = new ZipFile(apk.toFile())) {
                if (zip.getEntry("assets/haiagaru/image-vpn/installed") == null) throw new AssertionError("VPN marker missing");
            }
            System.out.println("PASS: patched APK contains both verified 4-ABI libraries and VPN marker");
        }
        for (var file : List.of(aar, vpn)) {
            var result = (Map<?,?>) method.invoke(null, file.toString(), vpnHashes, "libgojni.so");
            if (result.size() != 4) throw new AssertionError("Missing ABI");
        }
        method.invoke(null, image.toString(), imageHashes, "libtensorflowlite_jni.so");
        for (boolean wrongVpn : List.of(true, false)) {
            try {
                method.invoke(null, (wrongVpn ? image : vpn).toString(), wrongVpn ? vpnHashes : imageHashes, wrongVpn ? "libgojni.so" : "libtensorflowlite_jni.so");
                throw new AssertionError("Accepted wrong asset");
            } catch (java.lang.reflect.InvocationTargetException e) {
                if (!e.getCause().getMessage().contains("種類が違います")) throw e;
            }
        }
        var badHashes = new LinkedHashMap<>(vpnHashes);
        badHashes.replaceAll((abi, hash) -> "0".repeat(64));
        try {
            method.invoke(null, vpn.toString(), badHashes, "libgojni.so");
            throw new AssertionError("Accepted hash mismatch");
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (!e.getCause().getMessage().contains("対応するライブラリではありません")) throw e;
        }
        System.out.println("PASS: VPN ZIP/AAR (4 ABI each), Image NG ZIP, both wrong-file selections, SHA-256 rejection");
    }
}
