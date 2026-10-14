import app.morphe.engine.patches.PatchBundleLoader;
import app.morphe.patcher.patch.FilePathOption;
import java.io.File;
import java.util.List;
import java.util.Set;

/** Verify the actual bundle's picker metadata, not just the source declarations. */
public class VerifyIconFileOptions {
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException("Provide the built MPP");
        int checked = 0;
        for (var patch : PatchBundleLoader.INSTANCE.loadFlat(List.of(new File(args[0])))) {
            for (String key : List.of("appIcon", "edgeArchiveToolbarIconPath")) {
                if (!patch.getOptions().containsKey(key)) continue;
                var option = patch.getOptions().get(key);
                if (option == null) continue;
                if (!(option instanceof FilePathOption file))
                    throw new AssertionError(key + " is not a single-file option");
                if (!Set.copyOf(file.getAllowedExtensions()).equals(Set.of("png", "webp")))
                    throw new AssertionError(key + " extensions changed");
                if (file.getRequired() || !file.getDescription().contains("file path"))
                    throw new AssertionError(key + " optional/legacy picker metadata changed");
                System.out.println("PASS: " + key + " selects an optional PNG/WebP file");
                checked++;
            }
        }
        if (checked != 2) throw new AssertionError("Expected two registered icon options, found " + checked);
    }
}
