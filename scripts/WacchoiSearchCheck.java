package app.morphe.extension.chmate;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Run against the downloaded public day-index fixture as well as boundary cases. */
public final class WacchoiSearchCheck {
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        require(WacchoiSearch.validToken("o+Mz"), "plus is part of a slip");
        require(WacchoiSearch.validToken("/jDj"), "slash is part of a slip");
        require(!WacchoiSearch.validToken("Q8ld|"), "reject NG expression input");
        require(!WacchoiSearch.SLIP.matcher("xSdff-Q8ld").find(), "reject longer prefix");
        require(!WacchoiSearch.SLIP.matcher("Sdff-Q8ld/").find(), "reject longer suffix");
        Matcher symbols = WacchoiSearch.SLIP.matcher("(ﾜｯﾁｮｲ 4741-o+Mz) (ﾜｯﾁｮｲ bbaf-/jDj)");
        require(symbols.find() && symbols.group(2).equals("o+Mz"), "match plus");
        require(symbols.find() && symbols.group(2).equals("/jDj"), "match slash");
        require(WacchoiSearch.nextPage("<a href='./?p=9'>9</a><a href=./?p=2>2</a>", 1) == 2,
                "visit the next page rather than skipping to last");
        require(WacchoiSearch.nextPage("<a href=./?p=1>previous</a>", 2) == -1, "end of index");
        String base = "http://hissi.org/read.php/android/20261016/";
        require((base + "eHNXUllNb1lk.html").equals(
                WacchoiSearch.profileUrl(base, "./eHNXUllNb1lk.html")), "resolve profile path");
        require(WacchoiSearch.profileUrl(base, "https://example.org/advert.html") == null,
                "do not crawl unrelated links");
        List<String> allBoards = new ArrayList<>();
        for (int i = 0; i < 719; i++) allBoards.add("board" + i);
        Set<String> selectedBoards = new LinkedHashSet<>(List.of("android"));
        WacchoiSearch.selectAllBoards(allBoards, selectedBoards);
        require(selectedBoards.size() == allBoards.size()
                        && WacchoiSearch.allBoardsSelected(allBoards, selectedBoards),
                "select all available Hissi boards, not only the first visible page");
        selectedBoards.remove("board718");
        require(!WacchoiSearch.allBoardsSelected(allBoards, selectedBoards),
                "one deselected board disables whole-index scope");
        String html = Files.readString(Path.of(args[0]), Charset.forName("windows-31j"));
        Matcher rows = Pattern.compile("(?is)<tr\\b[^>]*>(.*?)</tr>").matcher(html);
        boolean upper = false, lower = false;
        while (rows.find()) {
            String row = rows.group(1);
            if (!row.contains("xsWRYMoYd")) continue;
            Matcher slip = WacchoiSearch.SLIP.matcher(row);
            while (slip.find()) {
                upper |= slip.group(1).equals("Sdff");
                lower |= slip.group(2).equals("Q8ld");
                require(!slip.group(2).equals("Q8LD"), "exact search is case sensitive");
            }
        }
        require(upper && lower, "find supplied poster in live public index with either half");
        System.out.println("PASS: symbol support, boundaries, pagination, all-board selection, URL resolution, live Sdff/Q8ld match");
    }
}
