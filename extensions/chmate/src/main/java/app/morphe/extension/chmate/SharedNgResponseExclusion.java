package app.morphe.extension.chmate;

import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.util.*;

/** A menu-target snapshot; no traversal of thread responses or modification of personal NGs. */
final class SharedNgResponseExclusion {
    private static final int ITEM = 0x7e484d01;
    private static final String TITLE = "このレスに一致する共有NGを除外";
    private static final Map<MenuItem, ProgrammableNgScript.Input> TARGETS = new WeakHashMap<>();

    private static ProgrammableNgScript.Input input(Object response, Object board) throws Exception {
        return ProgrammableNgController.sharedResponseInput(response, board);
    }

    static void append(Object object, Object response, Object board) {
        if (!SharedNg.enabled() || !(object instanceof Menu) || response == null || board == null) return;
        Menu menu = (Menu) object;
        if (menu.findItem(ITEM) != null) return;
        try {
            ProgrammableNgScript.Input target = input(response, board);
            MenuItem item = menu.add(Menu.NONE, ITEM, menu.size(), TITLE)
                    .setIcon(android.R.drawable.ic_menu_revert).setOnMenuItemClickListener(SharedNgResponseExclusion::handle);
            TARGETS.put(item, target);
        } catch (Exception error) { android.util.Log.w("Haiagaru", "Shared NG response menu unavailable", error); }
    }

    static boolean handle(Object object) {
        if (!(object instanceof MenuItem) || ((MenuItem) object).getItemId() != ITEM) return false;
        ProgrammableNgScript.Input target = TARGETS.get((MenuItem) object);
        Activity activity = Haiagaru.toolbarActivity(null);
        if (target != null && activity != null) SharedNg.showResponseExclusions(activity, target);
        return true;
    }

    static void appendSheet(Object builder, Object response, Object board) throws Exception {
        if (!SharedNg.enabled() || response == null || board == null) return;
        ProgrammableNgScript.Input target = input(response, board);
        ResponseArchive.appendSheetAction(builder, TITLE, android.R.drawable.ic_menu_revert, () -> {
            Activity activity = Haiagaru.toolbarActivity(null);
            if (activity != null) SharedNg.showResponseExclusions(activity, target);
        });
    }

    private SharedNgResponseExclusion() {}
}
