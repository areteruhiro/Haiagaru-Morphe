package android.widget;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

/** Test-only ListView fixture; never included in the extension. */
public class ListView {
    public static final int CHOICE_MODE_MULTIPLE = 2;
    public final List<?> rows;
    public final Set<Integer> checked = new HashSet<>();
    public int mode = 2;
    public ListView(List<?> rows) { this.rows = rows; }
    public int getChoiceMode() { return mode; }
    public int getCount() { return rows.size(); }
    public Object getItemAtPosition(int position) { return rows.get(position); }
    public boolean isItemChecked(int position) { return checked.contains(position); }
    public void setItemChecked(int position, boolean value) {
        if (value) checked.add(position); else checked.remove(position);
    }
}
