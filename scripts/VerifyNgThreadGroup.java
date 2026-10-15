import app.morphe.extension.chmate.NgThreadGroup;
import java.util.ArrayList;
import java.util.List;

public final class VerifyNgThreadGroup {
    public static void main(String[] args) {
        var source = new ArrayList<>(List.of("NG thread A", "NG thread B"));
        if (NgThreadGroup.visible(source, false) != source) throw new AssertionError("OFF must preserve identity");
        if (NgThreadGroup.visible(source, true) != null) throw new AssertionError("ON must suppress the entire bucket");
        if (!source.equals(List.of("NG thread A", "NG thread B"))) throw new AssertionError("Source was mutated");
        if (NgThreadGroup.visible(source, false) != source) throw new AssertionError("OFF must restore the group");
        for (boolean hidden : new boolean[]{true, false}) {
            if (NgThreadGroup.visible(null, hidden) != null) throw new AssertionError("null bucket");
        }
        var empty = List.of();
        if (NgThreadGroup.visible(empty, false) != empty) throw new AssertionError("empty bucket");
        System.out.println("PASS NG thread group: OFF, ON, source preservation, restore, null and empty");
    }
}
