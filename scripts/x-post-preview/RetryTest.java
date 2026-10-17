package app.morphe.extension.chmate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.widget.TextView;

/** Deterministic transient error, no real network. */
public final class RetryTest {
    public static String result = "pending";
    static TextView view;
    public static void start(Context context) {
        Haiagaru.enabled=true;
        XPostMetadata.calls=0;XPostMetadata.failures=1;
        XPostMetadata.response="{\"id_str\":\"456\",\"text\":\"自動再試行成功\",\"user\":{\"name\":\"検証\",\"screen_name\":\"fixture\"}}";
        view=new TextView(context) {
            @Override public boolean isShown(){return true;}
            @Override public boolean isAttachedToWindow(){return true;}
        };
        view.setText(new SpannableString("https://x.com/fixture/status/456"),TextView.BufferType.SPANNABLE);
        XPostPreview.apply(view);
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            result=XPostMetadata.calls==2 && view.getText().toString().contains("自動再試行成功")
                    ?"PASS transient failure retries visible preview without reopening"
                    :"FAIL calls="+XPostMetadata.calls+" body="+view.getText();
            view=null;
        },12_000);
    }
}
