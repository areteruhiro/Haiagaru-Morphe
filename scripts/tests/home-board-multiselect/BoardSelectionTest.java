package o;

import app.morphe.extension.chmate.HomeBoardMultiSelect;
import java.util.*;

/** JVM regression fixture mirroring the verified 241/242 model fields. */
public class BoardSelectionTest {
    static final class Flow {
        Object value;
        Flow(Object value) { this.value = value; }
        public Object b() { return value; }
        public Object d() { return value; }
    }
    static final class Selection {
        final Flow b, c;
        Selection() { b = c = new Flow(new HashSet<Long>()); }
        void toggle(long id) {
            Set<Long> updated = new HashSet<>((Set<Long>) b.value);
            if (!updated.add(id)) updated.remove(id);
            b.value = updated;
        }
        public void d(long id) { toggle(id); }
        public void b(long id) { toggle(id); }
    }
    static final class HomeViewModelFixture {
        final Flow f = new Flow(true), i = f, items;
        final Selection p = new Selection();
        HomeViewModelFixture(List<?> items) { this.items = new Flow(items); }
    }
    static final class Fragment { final HomeViewModelFixture model; Fragment(HomeViewModelFixture m) { model = m; } }
    static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }

    public static void main(String[] args) {
        android.widget.ListView legacy = new android.widget.ListView(Arrays.asList(
                new Yy2.Heading(2), new Yy2.Thread(), new Yy2.Thread(),
                new Yy2.Heading(4), new Yy2.Thread()));
        legacy.checked.add(1);
        legacy.checked.add(4);
        require(HomeBoardMultiSelect.selectLegacyRange(legacy, 0), "226 board heading not handled");
        require(legacy.checked.equals(new HashSet<>(Arrays.asList(1, 2, 4))), "226 partial selection not completed");
        require(HomeBoardMultiSelect.selectLegacyRange(legacy, 0), "226 deselect not handled");
        require(legacy.checked.equals(Collections.singleton(4)), "226 deselect escaped range");
        require(HomeBoardMultiSelect.selectLegacyRange(legacy, 3), "226 keyword heading not handled");
        require(legacy.checked.isEmpty(), "226 keyword deselect failed");
        require(!HomeBoardMultiSelect.selectLegacyRange(legacy, 1), "226 thread tap intercepted");
        legacy.mode = 0;
        require(!HomeBoardMultiSelect.selectLegacyRange(legacy, 0), "226 normal board tap intercepted");
        System.out.println("226: PASS");
        for (boolean modern : new boolean[]{false, true}) {
            BoardID board = new BoardID("a"), other = new BoardID("b");
            Object header = modern ? new getAvailabilityStatus.ComponentActivity(board) : new MediationBannerAdapter.read(board);
            Object nextHeader = modern ? new getAvailabilityStatus.RemoteActionCompatParcelizer() : new MediationBannerAdapter.RemoteActionCompatParcelizer();
            Object first = modern ? new getAvailabilityStatus.r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ(board, 11) : new MediationBannerAdapter.r8lambdavCwjfXDiSGcirCy4I008VOiJ_lw(board, 11);
            Object second = modern ? new getAvailabilityStatus.r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ(other, 12) : new MediationBannerAdapter.r8lambdavCwjfXDiSGcirCy4I008VOiJ_lw(other, 12);
            Object outside = modern ? new getAvailabilityStatus.r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ(board, 21) : new MediationBannerAdapter.r8lambdavCwjfXDiSGcirCy4I008VOiJ_lw(board, 21);
            Object endHeader = modern ? new getAvailabilityStatus.ComponentActivity(other) : new MediationBannerAdapter.read(other);
            HomeViewModelFixture model = new HomeViewModelFixture(Arrays.asList(header, first, first, second, nextHeader, outside, endHeader));
            Fragment fragment = new Fragment(model);
            ((Set<Long>) model.p.b.value).add(99L);
            require(HomeBoardMultiSelect.selectBoard(fragment, header), "header not handled");
            require(model.p.b.value.equals(new HashSet<>(Arrays.asList(11L, 12L, 99L))), "wrong bookmark IDs or other selection lost");
            require(HomeBoardMultiSelect.selectBoard(fragment, header), "deselect not handled");
            require(model.p.b.value.equals(Collections.singleton(99L)), "deselect changed another board");
            ((Set<Long>) model.p.b.value).add(11L);
            require(HomeBoardMultiSelect.selectBoard(fragment, header), "partial selection not handled");
            require(model.p.b.value.equals(new HashSet<>(Arrays.asList(11L, 12L, 99L))), "partial selection not completed");
            model.f.value = false;
            require(!HomeBoardMultiSelect.selectBoard(fragment, header), "normal board tap intercepted");
            model.f.value = true;
            require(!HomeBoardMultiSelect.selectBoard(fragment, first), "thread tap intercepted");
            require(HomeBoardMultiSelect.selectBoard(fragment, nextHeader), "keyword heading not handled");
            require(model.p.b.value.equals(new HashSet<>(Arrays.asList(11L, 12L, 21L, 99L))), "keyword range not selected");
            require(HomeBoardMultiSelect.selectBoard(fragment, nextHeader), "keyword deselect not handled");
            require(model.p.b.value.equals(new HashSet<>(Arrays.asList(11L, 12L, 99L))), "keyword deselection escaped range");
            require(!HomeBoardMultiSelect.selectBoard(fragment, endHeader), "empty group intercepted");
            System.out.println((modern ? "242" : "241") + ": PASS");
        }
    }
}

class Yy2 {
    static final class Heading { final int type; Heading(int type) { this.type = type; } public int a() { return type; } }
    static final class Thread { public int a() { return 1; } }
}

class MediationBannerAdapter {
    static final class read { final BoardID e; read(BoardID board) { e = board; } public long e() { return Long.MAX_VALUE; } }
    static final class RemoteActionCompatParcelizer { public long e() { return Long.MAX_VALUE; } }
    static final class r8lambdavCwjfXDiSGcirCy4I008VOiJ_lw {
        final BoardID c; final long b; final long m = 9001;
        r8lambdavCwjfXDiSGcirCy4I008VOiJ_lw(BoardID board, long id) { c = board; b = id; }
        public long a() { return m; }
        public long e() { return m; }
    }
}
class getAvailabilityStatus {
    static final class ComponentActivity { final BoardID a; ComponentActivity(BoardID board) { a = board; } public long a() { return Long.MAX_VALUE; } }
    static final class RemoteActionCompatParcelizer { public long a() { return Long.MAX_VALUE; } }
    static final class r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ {
        final BoardID e; final long a; final long n = 9002;
        r8lambdawJ5MHcSJed_CjC7r4OWD0UxyJsQ(BoardID board, long id) { e = board; a = id; }
        public long a() { return n; }
    }
}
class BoardID {
    final String value;
    BoardID(String value) { this.value = value; }
    public boolean equals(Object other) { return other instanceof BoardID && value.equals(((BoardID) other).value); }
    public int hashCode() { return value.hashCode(); }
}
