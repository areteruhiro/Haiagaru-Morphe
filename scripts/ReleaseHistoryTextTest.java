package app.morphe.extension.chmate;
import java.nio.file.*;
public final class ReleaseHistoryTextTest {
    static int checks;
    static void check(boolean v,String s){if(!v)throw new AssertionError(s);checks++;}
    public static void main(String[] args)throws Exception{
        String fixture="# Haiagaru 1.27.0 (2026-11-13)\n## 今回の変更\n- Cookie検索を改善\n## 安定版1.24.1からの全差分\n### 1.25.0\n- 板一覧を追加\n## 追加ファイル\n秘密ではない配布説明\n## 謝辞\n- 支援者\n### 🐛 Bug Fixes\n* regression fix\n# Haiagaru 1.27.0-dev.1 (2026-11-13)\n- 開発版の変更\n# Haiagaru 1.26.0 (2026-11-12)\n- 前回の変更\n# Haiagaru 1.24.1 (2026-11-06)\n- 基準版\n";
        var releases=ReleaseHistoryText.parse(fixture);
        check(releases.size()==3,"baseline excluded");
        check(!releases.get(0).development&&releases.get(1).development,"stable/dev distinction");
        check(releases.get(0).notes.contains("Cookie検索")&&releases.get(0).notes.contains("板一覧"),"full cumulative Japanese sections retained");
        check(!releases.get(0).notes.contains("支援者")&&!releases.get(0).notes.contains("配布説明"),"boilerplate excluded");
        check(releases.get(0).notes.contains("regression fix"),"generated category after boilerplate retained");
        check(ReleaseHistoryText.parse("Haiagaru 1.26.0 (2026-11-12)\n- 旧生成形式").size()==1,"legacy bundled headings");
        check(ReleaseHistoryText.parse("通信エラー").isEmpty(),"bad remote content rejected");
        var actual=ReleaseHistoryText.parse(Files.readString(Path.of(args[0])));
        check(actual.stream().anyMatch(r->r.version.equals("1.26.0")),"real stable release parsed");
        check(actual.stream().anyMatch(r->r.version.equals("1.25.0")),"previous stable parsed");
        check(actual.stream().noneMatch(r->r.version.equals("1.24.1")),"actual baseline cutoff");
        System.out.println("Release history: "+checks+" checks passed");
    }
}
