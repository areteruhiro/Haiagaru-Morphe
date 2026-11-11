package app.morphe.extension.chmate;

/** Pure parser regression checks; no Android runtime or public posting. */
public final class ThreadListThumbnailRulesTest {
    private static int checks;
    private static void check(boolean ok) {
        checks++;
        if (!ok) throw new AssertionError("thumbnail rule " + checks);
    }

    public static void main(String[] args) {
        var board = ThreadListThumbnailRules.board("https://egg.5ch.io/android/");
        check(board != null && board.name.equals("android") && board.host.equals("egg.5ch.io"));
        check(board.settingsKey().equals("5ch:egg.5ch.io/android"));
        check(board.legacySettingsKey().equals("5ch:android"));
        var sameFromThread = ThreadListThumbnailRules.board("https://egg.5ch.io/test/read.cgi/android/1790000000/");
        check(sameFromThread != null && sameFromThread.settingsKey().equals(board.settingsKey()));
        var differentHost = ThreadListThumbnailRules.board("https://eagle.5ch.net/android/");
        check(differentHost != null && !differentHost.settingsKey().equals(board.settingsKey()));
        check(board.threadUrl("1790000000").equals("https://egg.5ch.io/test/read.cgi/android/1790000000/"));
        var thread = ThreadListThumbnailRules.board("https://egg.5ch.io/test/read.cgi/android/1790000000/");
        check(thread != null && thread.name.equals("android"));
        var diagnostic = ThreadListThumbnailRules.board("{server:egg.5ch.io, name:android, created:123456}");
        check(diagnostic != null && diagnostic.name.equals("android"));
        check(ThreadListThumbnailRules.board("https://example.com/android/") == null);
        check(ThreadListThumbnailRules.board("https://fake5ch.io/android/") == null);
        check(ThreadListThumbnailRules.board("https://egg.5ch.io/android/?x=1") == null);
        check(ThreadListThumbnailRules.board("https://egg.5ch.io/test/read.cgi/android/not-a-thread/") == null);
        var edge = ThreadListThumbnailRules.board("https://bbs.eddibb.cc/liveedge/");
        check(edge != null && edge.host.equals("bbs.eddibb.cc") && edge.name.equals("liveedge"));
        check(edge.threadUrl("1790000000").equals("https://bbs.eddibb.cc/liveedge/1790000000"));
        check(ThreadListThumbnailRules.board("https://bbs.eddibb.cc/test/read.cgi/liveedge/1790000000/") != null);
        check(ThreadListThumbnailRules.board("http://bbs.eddibb.cc/liveedge/") != null);
        check(ThreadListThumbnailRules.board("https://bbs.eddibb.cc/liveedge/1790000000") != null);
        check(ThreadListThumbnailRules.board("https://bbs.eddibb.cc/test/read.cgi/other/1790000000/") == null);
        check(ThreadListThumbnailRules.board("https://bbs.eddibb.cc.evil.test/liveedge/") == null);
        check(ThreadListThumbnailRules.board("https://bbs.eddibb.cc/liveedge/?foo=1") == null);
        check(ThreadListThumbnailRules.publicImage("https://cdn.example.org/picture.jpeg"));
        check(!ThreadListThumbnailRules.publicImage("http://cdn.example.org/picture.jpeg"));
        check(!ThreadListThumbnailRules.publicImage("https://127.0.0.1/picture.jpeg"));
        check(!ThreadListThumbnailRules.publicImage("https://server.local/picture.jpeg"));
        String html = "<div class='thread'><div class='post'><div class='message'>"
            + "<a href='https://cdn.example.org/first.jpg'>first</a></div></div>"
            + "<div class='post'><div class='message'>"
            + "<a href='https://cdn.example.org/second.png'>second</a></div></div></div>";
        check("https://cdn.example.org/first.jpg".equals(
            ThreadListThumbnailRules.firstFiveChImage(html, board.threadUrl("1790000000"))));
        String secondOnly = "<div class='thread'><div class='post'><div class='message'>text</div></div>"
            + "<div class='post'><div class='message'><a href='https://cdn.example.org/second.png'>2</a></div></div></div>";
        check(ThreadListThumbnailRules.firstFiveChImage(secondOnly, board.threadUrl("1790000000")) == null);
        check("https://cdn.example.org/relative.webp".equals(
            ThreadListThumbnailRules.firstFiveChImage("<div class='post'><div class='message'>"
                + "<a href='https://cdn.example.org/relative.webp'>image</a></div></div>",board.threadUrl("1790000000"))));
        String edgeHtml="<div data-ng-target='body' data-ng-response-id='1'>"
                + "<span><span> https://i.imgur.com/IMje0I0.jpeg </span></span></div>"
                + "<div data-ng-target='body' data-ng-response-id='2'>https://cdn.example.org/second.png</div>";
        check("https://i.imgur.com/IMje0I0.jpeg".equals(
                ThreadListThumbnailRules.firstFiveChImage(edgeHtml,edge.threadUrl("1790000000"))));
        check(ThreadListThumbnailRules.firstFiveChImage(
                "<div data-ng-target='body' data-ng-response-id='1'>No image</div>"
                + "<div data-ng-target='body' data-ng-response-id='2'>https://cdn.example.org/second.png</div>",
                edge.threadUrl("1790000000"))==null);
        check("https://cdn.example.org/opening.webp".equals(ThreadListThumbnailRules.firstFiveChImage(
                "<div data-ng-target='body' data-ng-response-id='1'>"
                + "<a href='https://cdn.example.org/opening.webp'>image</a></div>",edge.threadUrl("1790000000"))));
        System.out.println("PASS " + checks + " thumbnail source checks");
    }
}
