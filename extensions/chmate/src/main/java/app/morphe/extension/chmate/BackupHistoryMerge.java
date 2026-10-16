package app.morphe.extension.chmate;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.JsonToken;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Stream posting arrays one record at a time, preserving both devices' histories. */
final class BackupHistoryMerge {
    static void posts(File local, InputStream incoming, OutputStream output) throws Exception {
        Set<String> seen = new HashSet<>();
        // Do not close output: its AtomicFile owner must commit/rollback it.
        Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
        writer.write('[');
        if (local.isFile()) try (InputStream input = new FileInputStream(local)) { appendPosts(input, writer, seen); }
        appendPosts(incoming, writer, seen);
        writer.write(']'); writer.flush();
    }

    private static void appendPosts(InputStream input, Writer output, Set<String> seen) throws Exception {
        JsonReader reader = new JsonReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        reader.beginArray();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        while (reader.hasNext()) {
            StringWriter text = new StringWriter();
            JsonWriter writer = new JsonWriter(text);
            value(reader, writer, 0); writer.flush();
            String record = text.toString();
            String key = android.util.Base64.encodeToString(digest.digest(record.getBytes(StandardCharsets.UTF_8)), android.util.Base64.NO_WRAP);
            if (seen.add(key)) {
                if (seen.size() > 1) output.write(',');
                output.write(record);
            }
        }
        reader.endArray();
        if (reader.peek() != JsonToken.END_DOCUMENT) throw new IOException("書き込み履歴の形式が不正です");
    }

    private static void value(JsonReader input, JsonWriter output, int depth) throws IOException {
        if (depth > 64) throw new IOException("書き込み履歴の階層が深すぎます");
        switch (input.peek()) {
            case BEGIN_OBJECT:
                input.beginObject(); output.beginObject();
                while (input.hasNext()) { output.name(input.nextName()); value(input, output, depth + 1); }
                input.endObject(); output.endObject(); break;
            case BEGIN_ARRAY:
                input.beginArray(); output.beginArray();
                while (input.hasNext()) value(input, output, depth + 1);
                input.endArray(); output.endArray(); break;
            case STRING: output.value(input.nextString()); break;
            case NUMBER: output.value(new java.math.BigDecimal(input.nextString())); break;
            case BOOLEAN: output.value(input.nextBoolean()); break;
            case NULL: input.nextNull(); output.nullValue(); break;
            default: throw new IOException("書き込み履歴の値が不正です");
        }
    }

    static void memo(File local, File incoming, OutputStream output) throws Exception {
        boolean samePrefix = true;
        try (InputStream a = new BufferedInputStream(new FileInputStream(local));
             InputStream b = new BufferedInputStream(new FileInputStream(incoming))) {
            long count = Math.min(local.length(), incoming.length());
            for (long i = 0; i < count; i++) if (a.read() != b.read()) { samePrefix = false; break; }
        }
        if (samePrefix) {
            try (InputStream input = new FileInputStream(local.length() >= incoming.length() ? local : incoming)) {
                HaiagaruBackupArchive.copy(input, output);
            }
        } else {
            // Keep whole records/line order; never deduplicate individual body lines.
            try (InputStream input = new FileInputStream(local)) { HaiagaruBackupArchive.copy(input, output); }
            if (local.length() > 0) {
                try (RandomAccessFile file = new RandomAccessFile(local, "r")) {
                    file.seek(file.length() - 1); if (file.read() != '\n') output.write('\n');
                }
            }
            try (InputStream input = new FileInputStream(incoming)) { HaiagaruBackupArchive.copy(input, output); }
        }
    }
}
