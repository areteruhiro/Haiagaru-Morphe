import app.morphe.patches.chmate.ImageCacheSizeOptions;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.w3c.dom.*;

public final class VerifyImageCacheOptions {
    static Document parse(String xml) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
    static void check(boolean value) { if (!value) throw new AssertionError(); }
    static String array(String name, String... items) {
        StringBuilder xml = new StringBuilder("<string-array name=\"" + name + "\">");
        for (String item : items) xml.append("<item>").append(item).append("</item>");
        return xml.append("</string-array>").toString();
    }
    public static void main(String[] args) throws Exception {
        String labels = array("labels", "300MB", "500MB", "1GB", "2GB");
        String values = array("values", "300", "500", "1024", "2048");
        Document doc = parse("<resources>" + labels + values + array("other", "1", "2") + "</resources>");
        List<String> names = ImageCacheSizeOptions.expand(doc);
        check(names.equals(List.of("labels", "values")));
        NodeList arrays = doc.getElementsByTagName("string-array");
        NodeList l = ((Element) arrays.item(0)).getElementsByTagName("item");
        NodeList v = ((Element) arrays.item(1)).getElementsByTagName("item");
        check(l.getLength() == 11 && v.getLength() == 11);
        check(l.item(3).getTextContent().equals("2GB"));
        for (int i = 4; i < 11; i++) {
            long mib = Long.parseLong(v.item(i).getTextContent());
            long gib = Long.parseLong(l.item(i).getTextContent().replace("GB", ""));
            check(mib * 1024L * 1024L == gib * 1024L * 1024L * 1024L);
            // Native trim arithmetic retains 85% using long, even at 256 GiB.
            check(mib * 89128960L / 100L > Integer.MAX_VALUE);
        }
        check(((Element) arrays.item(2)).getElementsByTagName("item").getLength() == 2);
        Document locale = parse("<resources>" + labels + values + "</resources>");
        ImageCacheSizeOptions.expandOverrides(locale, names);
        check(locale.getElementsByTagName("item").getLength() == 22);
        Document invalid = parse("<resources>" + labels + "</resources>");
        try { ImageCacheSizeOptions.expand(invalid); throw new AssertionError(); }
        catch (java.util.NoSuchElementException expected) { }
        check(invalid.getElementsByTagName("item").getLength() == 4);
        Document duplicate = parse("<resources>" + labels + labels + values + "</resources>");
        try { ImageCacheSizeOptions.expand(duplicate); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        check(duplicate.getElementsByTagName("item").getLength() == 12);
        Document badLocale = parse("<resources>" + labels + array("values", "1", "2", "3", "4") + "</resources>");
        try { ImageCacheSizeOptions.expandOverrides(badLocale, names); throw new AssertionError(); }
        catch (IllegalStateException expected) { }
        check(badLocale.getElementsByTagName("item").getLength() == 8);
        Document sanitized = parse("<resources>" + labels + array("values", "\\300", "\\500", "\\1024", "\\2048") + "</resources>");
        ImageCacheSizeOptions.expand(sanitized);
        NodeList escaped = ((Element) sanitized.getElementsByTagName("string-array").item(1)).getElementsByTagName("item");
        check(escaped.item(4).getTextContent().equals("\\4096"));
        check(escaped.item(10).getTextContent().equals("\\262144"));
        // Also run on actual decoded original resources when supplied.
        for (String path : args) {
            Document real = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path);
            check(ImageCacheSizeOptions.expand(real).size() == 2);
        }
        System.out.println("PASS image-cache choices, MiB mapping, >2GiB arithmetic, overrides, fail-closed layout");
    }
}
