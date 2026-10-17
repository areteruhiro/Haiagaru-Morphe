import app.morphe.extension.chmate.ThreadCommandRules;

public class VerifyThreadCommands {
    static int count;
    static void check(boolean condition) { if (!condition) throw new AssertionError("case " + count); count++; }
    public static void main(String[] args) {
        check(ThreadCommandRules.profile("https://egg.5ch.io/android/").equals("5ch"));
        check(ThreadCommandRules.profile("https://medaka.5ch.net/be/").equals("5ch"));
        check(ThreadCommandRules.profile("https://mercury.bbspink.com/test/").equals("5ch"));
        for (String h : new String[]{"d1ch.cc", "bbs.eddibb.cc", "v1ch.cc"}) check(ThreadCommandRules.profile("https://" + h + "/edge/").equals("edge"));
        for (String h : new String[]{"5ch.io.evil.com", "not5ch.net", "d1ch.cc.evil.com", "talk.jp"}) check(ThreadCommandRules.profile("https://" + h + "/").equals("custom"));
        check(ThreadCommandRules.profile("bad URL").equals("custom"));
        for (String p : new String[]{"5ch", "edge", "custom"}) check(ThreadCommandRules.labels(p).length == ThreadCommandRules.values(p).length);
        String command = "!metadent:vv:", draft = "本文\nURL https://example.org\n";
        check(ThreadCommandRules.insert(draft, command).equals(command + "\n" + draft));
        check(ThreadCommandRules.insert(command + "\n" + draft, command).equals(command + "\n" + command + "\n" + draft));
        check(ThreadCommandRules.conflicts("!metadent:v:\n本文", command));
        check(!ThreadCommandRules.conflicts(command + "\n本文", command));
        check(!ThreadCommandRules.conflicts("本文", "!custom:abc"));
        check(ThreadCommandRules.values("edge")[0].equals(command + "\n" + command + "\n" + command));
        for (String p : new String[]{"5ch", "edge"}) for (int i = 0; i < 2; i++) {
            String value = ThreadCommandRules.values(p)[i];
            check(value.split("\n", -1).length == 3);
            check(!ThreadCommandRules.conflicts(draft, value));
            check(!ThreadCommandRules.conflicts(value + "\n" + draft, value));
            check(ThreadCommandRules.insert(ThreadCommandRules.insert(draft,value),value).equals(value+"\n"+value+"\n"+draft));
            check(ThreadCommandRules.editValue(value,value.split("\n")[0]).equals(value));
            check(ThreadCommandRules.editValue(value,null).equals(value));
            check(ThreadCommandRules.editValue(value,"!custom\n!other").equals("!custom\n!other"));
        }
        check(ThreadCommandRules.normalize("  !one\r\n!two\r!three\n").equals("!one\n!two\n!three"));
        check(ThreadCommandRules.insert(draft,"!one\r\n!two").equals("!one\n!two\n"+draft));
        check(ThreadCommandRules.conflicts("!metadent:v:\n本文",ThreadCommandRules.values("edge")[0]));
        check(ThreadCommandRules.conflicts("本文","!metadent:vv:\n!metadent:v:"));
        check(!ThreadCommandRules.conflicts("本文","!one\n!two\n!one"));
        check(ThreadCommandRules.values("edge")[2].equals("!metadent:v:"));
        for (String p : new String[]{"open2ch", "shitaraba", "afternoontea", "custom"})
            check(ThreadCommandRules.labels(p).length == ThreadCommandRules.values(p).length);
        check(ThreadCommandRules.profile("https://hayabusa.open2ch.net/livejupiter/").equals("open2ch"));
        check(ThreadCommandRules.profile("https://jbbs.shitaraba.net/game/1/").equals("shitaraba"));
        check(ThreadCommandRules.profile("https://afternoontea.st/boards/1/").equals("afternoontea"));
        check(ThreadCommandRules.profile("https://open2ch.net.evil.com/").equals("custom"));
        check(ThreadCommandRules.boardKey("https://egg.5ch.io/test/read.cgi/android/123/1").equals("egg.5ch.io/android/"));
        check(ThreadCommandRules.boardKey("https://jbbs.shitaraba.net/bbs/read.cgi/game/123/456/").equals("jbbs.shitaraba.net/game/123/"));
        check(ThreadCommandRules.boardKey("https://afternoontea.st/boards/123/").equals("afternoontea.st/boards/123/"));
        check(ThreadCommandRules.boardKey("https://user@egg.5ch.io/android/").isEmpty());
        check(ThreadCommandRules.boardKey("file:///android/").isEmpty());
        check(ThreadCommandRules.settingsUrl("http://egg.5ch.io/test/read.cgi/android/123/").equals("https://egg.5ch.io/android/SETTING.TXT"));
        check(ThreadCommandRules.settingsUrl("https://egg.5ch.io/android/SETTING.TXT").equals("https://egg.5ch.io/android/SETTING.TXT"));
        check(ThreadCommandRules.settingsUrl("https://d1ch.cc/edge/").isEmpty());
        check(ThreadCommandRules.settingsUrl("https://5ch.io.evil.com/android/").isEmpty());
        var settings = ThreadCommandRules.settings("BBS_USE_VIPQ2=2\r\nBBS_FORCE_ID=checked\nEVIL=hidden\nBBS_LINE_NUMBER=20\n");
        check(settings.size() == 3);
        check(settings.get("BBS_USE_VIPQ2").equals("2"));
        check(settings.get("BBS_LINE_NUMBER").equals("20"));
        check(!ThreadCommandRules.available("5ch",0,false));
        check(ThreadCommandRules.available("5ch",13,false));
        check(ThreadCommandRules.nameCommand("5ch",15));
        check(!ThreadCommandRules.nameCommand("edge",13));
        check(ThreadCommandRules.available("open2ch",0,true));
        check(!ThreadCommandRules.available("open2ch",2,true));
        check(ThreadCommandRules.available("open2ch",2,false));
        check(!ThreadCommandRules.available("open2ch",0,false));
        System.out.println("PASS " + count + " thread-command cases");
    }
}
