package app.morphe.extension.chmate;

import android.view.Menu;
import android.view.MenuItem;

/** Only suppress the native action; never change the NG model or force hidden actions visible. */
public final class HiddenNgMenu {
    private HiddenNgMenu() { }

    /** Replaced by the patch with the verified native menu resource ID for this APK. */
    public static int itemId() { return 0; }

    public static void apply(Menu menu) {
        if (menu == null || !Haiagaru.hideShowHiddenNgMenu()) return;
        MenuItem item = menu.findItem(itemId());
        if (item != null) item.setVisible(false);
    }
}
