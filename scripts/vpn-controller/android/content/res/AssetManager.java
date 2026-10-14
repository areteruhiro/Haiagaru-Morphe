package android.content.res;
import java.io.*;
public class AssetManager {
    private final boolean installed;
    public AssetManager(boolean installed) { this.installed = installed; }
    public InputStream open(String path) throws IOException {
        if (!path.equals("haiagaru/image-vpn/installed") || !installed) throw new IOException("Missing marker");
        return new ByteArrayInputStream(new byte[]{'1'});
    }
}
