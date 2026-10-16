package app.morphe.extension.chmate;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.*;
import java.util.*;

/** Explicit multi-board selection. Opening/cancelling the picker changes no rules. */
final class NgBoardScopePicker extends LinearLayout {
    private final Spinner mode;
    private final Button choose;
    private final Set<String> known = new TreeSet<>(), selected = new TreeSet<>();
    private Runnable changed = () -> {};

    NgBoardScopePicker(Context context, String currentBoard, String initialScope) {
        super(context);
        setOrientation(VERTICAL);
        TextView label = new TextView(context); label.setText("対象板"); addView(label);
        if (currentBoard != null) known.addAll(NgBoardScope.boards(currentBoard));
        for (String category : new String[]{"NAME", "WORD", "ID", "THREAD", "BE", "IMAGE"}) {
            try {
                for (NativeNgStore.Entry entry : new NativeNgStore(context, category).entries())
                    known.addAll(NgBoardScope.boards(entry.board));
            } catch (Exception error) { android.util.Log.w("Haiagaru", "NG board choices: " + category, error); }
        }
        selected.addAll(NgBoardScope.boards(initialScope)); known.addAll(selected);
        mode = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item,
                new String[]{"全板共通", "選択した板のみ", "選択した板以外すべて"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mode.setAdapter(adapter); addView(mode);
        choose = new Button(context); choose.setAllCaps(false); addView(choose);
        choose.setOnClickListener(v -> selectBoards());
        mode.setSelection(initialScope == null ? 0 : NgBoardScope.isExcluded(initialScope) ? 2 : 1);
        mode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { refresh(); changed.run(); }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        refresh();
    }

    void onChanged(Runnable listener) { changed = listener; }

    String value() {
        if (mode.getSelectedItemPosition() == 0) return null;
        return NgBoardScope.encode(mode.getSelectedItemPosition() == 2, selected);
    }

    private void refresh() {
        choose.setVisibility(mode.getSelectedItemPosition() == 0 ? GONE : VISIBLE);
        choose.setText((mode.getSelectedItemPosition() == 2 ? "除外する板" : "対象の板")
                + "を選択（" + selected.size() + "件）\n" + String.join(", ", selected));
    }

    private void selectBoards() {
        LinearLayout rows = new LinearLayout(getContext()); rows.setOrientation(VERTICAL);
        Set<String> pending = new TreeSet<>(selected);
        for (String board : known) {
            CheckBox check = new CheckBox(getContext()); check.setText(board); check.setChecked(pending.contains(board));
            check.setOnCheckedChangeListener((button, checked) -> { if (checked) pending.add(board); else pending.remove(board); });
            rows.addView(check);
        }
        TextView help = new TextView(getContext()); help.setText("タップで選択／解除。候補にない板は板IDを追加してください（例: android, poverty）。"); rows.addView(help);
        EditText extra = new EditText(getContext()); extra.setHint("追加する板ID（カンマ・改行区切り）"); rows.addView(extra);
        ScrollView scroll = new ScrollView(getContext()); scroll.addView(rows);
        AlertDialog dialog = new AlertDialog.Builder(getContext()).setTitle(mode.getSelectedItemPosition() == 2 ? "除外する板" : "対象の板")
                .setView(scroll).setNegativeButton("キャンセル", null).setPositiveButton("OK", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                Set<String> result = new TreeSet<>(pending);
                for (String board : extra.getText().toString().split("[,\\s]+")) if (!board.isEmpty()) result.add(board);
                NgBoardScope.encode(mode.getSelectedItemPosition() == 2, result);
                selected.clear(); selected.addAll(result); known.addAll(result); refresh(); changed.run(); dialog.dismiss();
            } catch (IllegalArgumentException error) { extra.setError(error.getMessage()); }
        }));
        dialog.show();
    }
}
