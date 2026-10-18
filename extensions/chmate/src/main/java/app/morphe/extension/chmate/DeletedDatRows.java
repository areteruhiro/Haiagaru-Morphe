package app.morphe.extension.chmate;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Preserve one response for an erased, whole DAT row, without recovering its text. */
final class DeletedDatRows {
    private static final byte[] PLACEHOLDER = "aboon<><><> [deleted] <>".getBytes(StandardCharsets.US_ASCII);

    static int copy(InputStream source, OutputStream target) throws IOException {
        boolean boundary = false; // Never invent a missing first/title row.
        long zeros = 0;
        int repaired = 0, value;
        while ((value = source.read()) != -1) {
            if (boundary && value == 0) { zeros++; continue; }
            if (zeros > 0) {
                if (zeros >= PLACEHOLDER.length + 1 && value != '\n' && value != '\r') {
                    target.write(PLACEHOLDER);
                    for (long i = PLACEHOLDER.length; i < zeros - 1; i++) target.write(' ');
                    target.write('\n');
                    repaired++;
                } else for (long i = 0; i < zeros; i++) target.write(0);
                zeros = 0;
            }
            target.write(value);
            boundary = value == '\n';
        }
        // A trailing zero run may be a partial write. Do not fabricate a response.
        for (long i = 0; i < zeros; i++) target.write(0);
        return repaired;
    }
}
