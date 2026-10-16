import app.morphe.extension.chmate.WacchoiMenuOrder;
import java.util.*;

public final class VerifyWacchoiMenuOrder {
    record Action(String title, Runnable callback) { }
    public static void main(String[] args) {
        var actions = new ArrayList<Action>();
        String[] original = {"必死チェッカーもどき(ﾜｯﾁｮｲ)", "ﾜｯﾁｮｲ下4桁でNGName登録", "NGName に追加",
                "コピー", "取得済みのスレから検索", "その他A", "その他B"};
        for (String title : original) actions.add(new Action(title, () -> {}));
        var before = new ArrayList<>(actions);
        WacchoiMenuOrder.reorder(actions, Action::title);
        if (!actions.stream().map(Action::title).toList().equals(List.of("NGName に追加", "ﾜｯﾁｮｲ下4桁でNGName登録",
                "コピー", "取得済みのスレから検索", "必死チェッカーもどき(ﾜｯﾁｮｲ)", "その他A", "その他B")))
            throw new AssertionError("Wrong order");
        for (var action : before) if (actions.stream().noneMatch(a -> a == action))
            throw new AssertionError("Action/callback replaced");
        var twice = new ArrayList<>(actions);
        WacchoiMenuOrder.reorder(actions, Action::title);
        if (!actions.equals(twice)) throw new AssertionError("Repeated append ordering");
        if (WacchoiMenuOrder.rank("NGNameを解除") != 5 || WacchoiMenuOrder.rank("NGIDに追加") != 5)
            throw new AssertionError("Unrelated NG action classified as add NGName");
        if (WacchoiMenuOrder.rank("ﾜｯﾁｮｲで検索") != 4 || WacchoiMenuOrder.rank("ﾜｯﾁｮｲ下4桁をNG登録") != 1)
            throw new AssertionError("Legacy labels");
        if (WacchoiMenuOrder.rank("ﾜｯﾁｮｲ下4桁で検索") != 4)
            throw new AssertionError("Suffix search must not be grouped with NG registration");
        System.out.println("PASS Wacchoi menu order, action identity, extras, repeated append and legacy labels");
    }
}
