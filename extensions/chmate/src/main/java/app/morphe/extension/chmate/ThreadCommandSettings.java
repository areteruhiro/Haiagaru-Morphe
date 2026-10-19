package app.morphe.extension.chmate;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Executors;

/** Read-only, explicit board-setting lookup. Never changes a draft or posts a command. */
final class ThreadCommandSettings {
    private static final java.util.concurrent.ExecutorService IO = Executors.newSingleThreadExecutor();
    static String fetch(String address) throws Exception {
        String allowed = ThreadCommandRules.settingsUrl(address);
        if (!address.equals(allowed)) throw new IOException("Unsupported settings URL");
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new IOException("HTTP " + status);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream stream = connection.getInputStream()) {
                byte[] buffer = new byte[4096]; int n;
                while ((n = stream.read(buffer)) != -1) {
                    if (bytes.size() + n > 131072) throw new IOException("SETTING.TXT is too large");
                    bytes.write(buffer, 0, n);
                }
            }
            String type = connection.getContentType();
            String text = new String(bytes.toByteArray(), Charset.forName(type != null && type.toLowerCase(java.util.Locale.ROOT).contains("utf-8") ? "UTF-8" : "MS932"));
            Map<String, String> settings = ThreadCommandRules.settings(text);
            if (settings.isEmpty()) throw new IOException("SETTING.TXTの設定値を確認できませんでした");
            StringBuilder result = new StringBuilder();
            for (Map.Entry<String, String> entry : settings.entrySet()) result.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
            String vip = settings.get("BBS_USE_VIPQ2");
            int enabled = 0; try { enabled = Integer.parseInt(vip); } catch (Exception ignored) { }
            result.append(enabled >= 2 ? "\n!extendの利用条件（VIPQ2が2以上）を満たしています。" : "\n!extendの利用条件（VIPQ2が2以上）を確認できません。候補の効果は保証されません。");
            return result.toString();
        } finally { connection.disconnect(); }
    }
    static void show(Context context, String boardUrl) {
        String address = ThreadCommandRules.settingsUrl(boardUrl);
        if (address.isEmpty()) return;
        HaiagaruDialog progress = new HaiagaruDialog.Builder(context).setTitle("板設定を取得")
                .setMessage(address + "\n取得中…").setNegativeButton("閉じる", null).show();
        IO.execute(() -> {
            String result;
            try { result = fetch(address); }
            catch (Exception error) { result = "取得できませんでした。候補や下書きは変更しません。\n" + error.getMessage(); }
            final String message = result;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (!progress.isShowing()) return;
                progress.dismiss();
                new HaiagaruDialog.Builder(context).setTitle("板設定（読み取り専用）")
                        .setMessage(address + "\n\n" + message + "\n\n値を自動でコマンドとして実行・挿入することはありません。")
                        .setPositiveButton("閉じる", null).show();
            });
        });
    }
}
