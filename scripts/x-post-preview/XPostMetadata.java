package app.morphe.extension.chmate;
// Network is deliberately unavailable in this fixture.
final class XPostMetadata {
    static String fetch(String id) { throw new AssertionError("Unexpected network request"); }
}
