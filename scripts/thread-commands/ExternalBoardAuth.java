package app.morphe.extension.chmate;

// Isolated command-dialog tests never attach to a real posting target.
final class ExternalBoardAuth {
    static void target(Object fragment, String url) {}
}
