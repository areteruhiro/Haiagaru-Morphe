package app.morphe.extension.chmate;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import app.morphe.extension.chmate.vpn.sstp.SstpImageSession;

/** Debug APK only. No tracker URLs and no automatic image fetching. */
public final class ImageVpnProbeActivity extends Activity {
    private volatile SstpImageSession active;
    private volatile ImageVpnRelay relay;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView status = new TextView(this);
        status.setText("VPN Gate diagnostic: fixed exit-address probe only");
        setContentView(status);
        new Thread(() -> {
            try {
                for (VpnGateCatalog.Server server : VpnGateCatalog.fetch()) {
                    NativeImagePacketStack packets = new NativeImagePacketStack();
                    SstpImageSession candidate = new SstpImageSession(getApplicationContext(), server.ip,
                            server.hostname, server.port, server.trustAnchorPem, packets);
                    active = candidate;
                    Log.i("HaiagaruVpnProbe", "CONNECT " + server.hostname + " " + server.ip + ":" + server.port);
                    try {
                        candidate.start();
                        candidate.awaitReady(20000);
                        Log.i("HaiagaruVpnProbe", "PPP_READY");
                        String exit = packets.probeExitAddress();
                        Log.i("HaiagaruVpnProbe", "EXIT_ADDRESS " + exit);
                        runOnUiThread(() -> status.setText("Private tunnel exit address: " + exit));
                        String image = getIntent().getStringExtra("image");
                        if (image != null) {
                            if (!ImageVpnRequestPolicy.isProtectedUrl(image)) throw new IllegalArgumentException("Invalid image probe target");
                            candidate.close();
                            relay = new ImageVpnRelay(ImageVpnFetchManager.getInstance(getApplicationContext()));
                            String local = relay.register(image);
                            HttpURLConnection download = (HttpURLConnection)new URL(local).openConnection(Proxy.NO_PROXY);
                            download.setConnectTimeout(5000); download.setReadTimeout(110000);
                            try {
                                if (download.getResponseCode() != 200) throw new IllegalStateException("Relay HTTP " + download.getResponseCode());
                                ByteArrayOutputStream data = new ByteArrayOutputStream();
                                try (InputStream input = download.getInputStream()) {
                                    byte[] buffer = new byte[8192]; int count;
                                    while ((count = input.read(buffer)) >= 0) {
                                        if (data.size() + count > 32 * 1024 * 1024) throw new IllegalStateException("Oversize image");
                                        data.write(buffer, 0, count);
                                    }
                                }
                                byte[] bytes = data.toByteArray();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                if (bitmap == null) throw new IllegalStateException("Image decode failed");
                                Log.i("HaiagaruVpnProbe", "RELAY_IMAGE_OK bytes=" + bytes.length + " dimensions=" + bitmap.getWidth() + "x" + bitmap.getHeight());
                                runOnUiThread(() -> {
                                    ImageView view = new ImageView(this);
                                    view.setImageBitmap(bitmap);
                                    view.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                    setContentView(view);
                                });
                            } finally { download.disconnect(); relay.close(); }
                        }
                        return;
                    } catch (Throwable failed) {
                        Log.e("HaiagaruVpnProbe", "CANDIDATE_FAILED", failed);
                    } finally { candidate.close(); }
                }
                Log.e("HaiagaruVpnProbe", "NO_VERIFIED_TUNNEL");
            } catch (Throwable failed) { Log.e("HaiagaruVpnProbe", "PROBE_FAILED", failed); }
        }, "HaiagaruVpnProbe").start();
    }
    @Override public void onDestroy() {
        if (active != null) active.close();
        if (relay != null) relay.close();
        super.onDestroy();
    }
}
