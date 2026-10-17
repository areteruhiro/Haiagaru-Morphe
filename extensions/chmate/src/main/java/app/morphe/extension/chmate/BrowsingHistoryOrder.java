package app.morphe.extension.chmate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;

/** Display-only ordering: never writes history, bookmarks or DAT. */
public final class BrowsingHistoryOrder {
    private BrowsingHistoryOrder() { }
    public interface Access<T> {
        String board(T row) throws Exception;
        long visited(T row) throws Exception;
    }
    public static long cutoff(long now, int days, TimeZone zone) {
        if (days == 0) return 0;
        Calendar date = Calendar.getInstance(zone);
        date.setTimeInMillis(now);
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DATE, -days);
        return date.getTimeInMillis();
    }
    public static <T> List<T> arrange(List<T> input, Access<? super T> access, long cutoff) throws Exception {
        final class Row {
            final T value; final String board; final long visited;
            Row(T value, String board, long visited) {
                this.value = value; this.board = board; this.visited = visited;
            }
        }
        ArrayList<Row> rows = new ArrayList<>();
        for (T value : input) {
            String board = access.board(value);
            // A null board denotes a native section heading, not a thread.
            if (board == null) continue;
            long visited = access.visited(value);
            if (visited > 0 && visited >= cutoff) rows.add(new Row(value, board, visited));
        }
        // Stable sorting retains native order when timestamps tie.
        Collections.sort(rows, (a, b) -> Long.compare(b.visited, a.visited));
        LinkedHashMap<String, List<T>> boards = new LinkedHashMap<>();
        for (Row row : rows) {
            List<T> group = boards.get(row.board);
            if (group == null) { group = new ArrayList<>(); boards.put(row.board, group); }
            group.add(row.value);
        }
        ArrayList<T> result = new ArrayList<>();
        for (List<T> group : boards.values()) result.addAll(group);
        return result;
    }
}
