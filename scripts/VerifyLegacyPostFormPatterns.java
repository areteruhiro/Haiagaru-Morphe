import app.morphe.extension.chmate.LegacyPostFormPatterns;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mirrors 191's native two-stage parser, using the production pattern factory. */
public final class VerifyLegacyPostFormPatterns {
    private static Map<String,String> parse(String html, boolean fixed) {
        String tagExpression = "<input (.*?)>";
        String attributeExpression = "(\\S+)=(?:\"(.*?)\"|([^\\s\"]+))";
        Pattern tags = fixed ? LegacyPostFormPatterns.compile(tagExpression) : Pattern.compile(tagExpression);
        Pattern attributes = fixed ? LegacyPostFormPatterns.compile(attributeExpression) : Pattern.compile(attributeExpression);
        Map<String,String> fields = new LinkedHashMap<>();
        Matcher tag = tags.matcher(html);
        while (tag.find()) {
            Matcher attribute = attributes.matcher(tag.group(1));
            String name = null, value = null;
            while (attribute.find()) {
                String content = attribute.group(2) != null ? attribute.group(2) : attribute.group(3);
                if ("name".equalsIgnoreCase(attribute.group(1))) name = content;
                if ("value".equalsIgnoreCase(attribute.group(1))) value = content;
            }
            if (name != null && value != null) fields.put(name,value);
        }
        return fields;
    }
    public static void main(String[] args) {
        String body = "first line\nsecond line\r\nhttps://example.com/";
        String html = "<input type=hidden name=FROM value=\"\">"
            + "<input type=hidden name=MESSAGE value=\"" + body + "\">"
            + "<input type=hidden name=feature value=confirmed>";
        if (parse(html,false).containsKey("MESSAGE")) throw new AssertionError("old parser must reproduce failure");
        Map<String,String> fields = parse(html,true);
        if (!body.equals(fields.get("MESSAGE")) || !"".equals(fields.get("FROM"))
            || !"confirmed".equals(fields.get("feature"))) throw new AssertionError(fields.keySet().toString());
        String simple = "<input name=MESSAGE value=\"single line\">";
        if (!parse(simple,false).equals(parse(simple,true))) throw new AssertionError("single-line regression");
        for (String destination : new String[]{"https://example.org/test/bbs.cgi", "https://talk.jp/",
                "https://egg.5ch.io/test/bbs.cgi", "https://bbs.example.net/post"}) {
            String external = "<form method=POST action=\"" + destination + "\">" + html + "</form>";
            if (!body.equals(parse(external,true).get("MESSAGE")))
                throw new AssertionError("board-dependent parser: " + destination);
        }
        if (!parse("<input\n name=MESSAGE value=\"x\">",true).isEmpty())
            throw new AssertionError("do not broaden unrelated tag syntax");
        System.out.println("PASS: multiline failure reproduced; LF/CRLF body and fields preserved; no destination/domain restriction");
    }
}
