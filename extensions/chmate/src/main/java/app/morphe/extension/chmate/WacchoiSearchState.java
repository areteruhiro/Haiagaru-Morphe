package app.morphe.extension.chmate;

import android.content.Intent;

/** Condition snapshots only: returning from a notification never starts a search. */
final class WacchoiSearchState {
    static final String RESTORE = "haiagaru.restoreSearch";
    static Intent copy(Intent source) {
        return source == null ? new Intent() : new Intent(source);
    }
    static Intent notificationArguments(Intent source) {
        return new Intent().putExtras(copy(source)).setAction("haiagaru.SHOW_SEARCH")
                .putExtra(RESTORE,true)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }
    private WacchoiSearchState() { }
}
