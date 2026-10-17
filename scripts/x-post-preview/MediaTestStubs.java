package app.morphe.extension.chmate;

// Preview tests exercise the real URL parser, not video downloading or serving.
final class VideoFrameThumbnail {
    static void request(String url) {}
    static boolean fourAllowed() { return false; }
    static String currentVariant() { return "single-v1"; }
}
final class VideoThumbnailServer {
    static String placeholder() { return ""; }
}
