package app.morphe.extension.chmate;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class VerifyHissiThreadResults {
    public static void main(String[] args) throws Exception {
        String html = Files.readString(Path.of(args[0]), Charset.forName("windows-31j"));
        List<HissiThreadResults.Result> rows = HissiThreadResults.parse(html);
        HissiThreadResults.Result target = rows.stream()
                .filter(r -> r.url.endsWith("/1792102288/10")).findFirst().orElseThrow();
        if (!target.titleHtml.contains("Haiagaru") || !target.bodyHtml.contains("mp4"))
            throw new AssertionError("Title and matching post content must stay paired");
        if (rows.stream().anyMatch(r -> r.url.endsWith("/l50") || r.bodyHtml.isEmpty()))
            throw new AssertionError("Prefer actual posts over summary links");
        if (HissiThreadResults.parse("<dl><a href='https://evil.example/test/read.cgi/b/1/'>bad</a><dd>x</dl>").size() != 0)
            throw new AssertionError("Unrelated links excluded");
        String fallback = "<a href='https://egg.5ch.io/test/read.cgi/android/123/l50'>Title &amp; more</a>";
        List<HissiThreadResults.Result> old = HissiThreadResults.parse(fallback + fallback);
        if (old.size() != 1 || !old.get(0).bodyHtml.isEmpty() || !old.get(0).titleHtml.contains("Title"))
            throw new AssertionError("Summary-only fallback and deduplication");
        if (!HissiThreadResults.parse("").isEmpty()) throw new AssertionError("Empty profile");
        System.out.println("PASS: " + rows.size() + " real posts, title/body pairing, summary fallback, duplicate and unrelated links");
    }
}
