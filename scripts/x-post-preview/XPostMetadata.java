package app.morphe.extension.chmate;
// Network is deliberately unavailable in this fixture.
final class XPostMetadata {
    static void clear() { }
    static String fetchPreview(String id) throws java.io.IOException { return fetch(id); }
    static String response;
    static int failures, calls;
    static String fetch(String id) throws java.io.IOException {
        if (response == null) throw new AssertionError("Unexpected network request");
        ++calls;
        if (failures-- > 0) throw new java.io.IOException("Synthetic transient failure");
        return response;
    }
}
