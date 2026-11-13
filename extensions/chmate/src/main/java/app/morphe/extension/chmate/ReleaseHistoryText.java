package app.morphe.extension.chmate;

import java.util.*;
import java.util.regex.*;

/** Parses both bundled notes and GitHub's full changelog into individual releases. */
final class ReleaseHistoryText {
    static final class Release {
        final String version, date, notes;
        final boolean development;
        Release(String version, String date, String notes) {
            this.version=version;this.date=date;this.notes=notes;
            development=version.contains("-dev.");
        }
    }
    private static final Pattern HEADER=Pattern.compile("(?m)^(?:# )?Haiagaru ([0-9]+\\.[0-9]+\\.[0-9]+(?:-[A-Za-z0-9.]+)?) \\(([^)]+)\\)\\s*$");
    static List<Release> parse(String source) {
        List<Release> result=new ArrayList<>();
        Matcher headings=HEADER.matcher(source);int start=-1;String version=null,date=null;
        while(headings.find()) {
            if(start>=0)result.add(new Release(version,date,notes(source.substring(start,headings.start()))));
            if(headings.group(1).equals("1.24.1")) { start=-1;break; }
            version=headings.group(1);date=headings.group(2);start=headings.end();
        }
        if(start>=0)result.add(new Release(version,date,notes(source.substring(start))));
        return result;
    }
    private static String notes(String source) {
        StringBuilder result=new StringBuilder();boolean excluded=false;
        for(String raw:source.split("\\r?\\n")) {
            String line=raw.trim();
            if(line.startsWith("## ")) {
                excluded=line.equals("## 追加ファイル")||line.equals("## 謝辞")||line.equals("## 開発継続へのご支援のお願い");
            } else if(line.startsWith("### ")&&(line.contains("Bug Fixes")||line.contains("New Features")||line.contains("Improvements")||line.contains("Updated App Support"))) excluded=false;
            if(excluded||line.equals("今回の変更内容は、以下のカテゴリ別リリース項目をご覧ください。"))continue;
            line=line.replace("🐛 Bug Fixes","修正").replace("✨ New Features","追加機能")
                .replace("🛠 Improvements & Fixes","改善・修正").replace("🔧 Improvements","改善")
                .replace("🚀 Updated App Support","対応アプリ");
            line=ReleaseNoteJapanese.line(line);
            if(line.equals("## 主な変更"))continue;
            result.append(line).append('\n');
        }
        String text=result.toString().trim();
        return text.isEmpty()?"詳細はGitHubのリリースページをご覧ください。":text;
    }
    private ReleaseHistoryText(){}
}
