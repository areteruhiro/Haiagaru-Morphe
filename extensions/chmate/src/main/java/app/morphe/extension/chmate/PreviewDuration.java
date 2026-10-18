package app.morphe.extension.chmate;

/** A single minutes:seconds value, preserving the existing seconds preference. */
final class PreviewDuration {
    static String format(int seconds) {
        return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
    }
    static int parse(String value) {
        String[] parts = value.trim().split(":", -1);
        if (parts.length != 2 || !parts[0].matches("[0-9]{1,4}") || !parts[1].matches("[0-9]{1,2}"))
            throw new IllegalArgumentException();
        int minutes = Integer.parseInt(parts[0]), seconds = Integer.parseInt(parts[1]);
        if (minutes > 1440 || seconds > 59) throw new IllegalArgumentException();
        return minutes * 60 + seconds;
    }
}
