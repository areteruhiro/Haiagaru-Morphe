package app.morphe.extension.chmate;

import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;
import java.io.*;

/** Synthetic visual sample, no connection to X or real engagement counts. */
public final class CardRenderTest {
    public static String render(Context context) throws Exception {
        Bitmap bitmap=Bitmap.createBitmap(1080,950,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(0xff17221e);
        TextView view=new TextView(context);
        view.setTextColor(0xffe7e9ea);view.setTextSize(16);
        view.setPadding(24,20,24,20);
        view.setLayoutParams(new android.view.ViewGroup.LayoutParams(1080,950));
        SpannableStringBuilder text=new SpannableStringBuilder("167 名無しさん\nhttps://x.com/example/status/123\n\n");
        int start=text.length();
        text.append("サンプル投稿者  · X\n@example\n\n");
        text.setSpan(new StyleSpan(Typeface.BOLD),start,text.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.append("これは表示確認用の投稿です。\n投稿者・本文・日時を、スレの中でカード形式で表示します。\n\n2026/10/18 12:00\n\n投稿を開く");
        XPostPreviewCard.add(text,start,view);
        view.setText(text,TextView.BufferType.SPANNABLE);
        view.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(950,View.MeasureSpec.EXACTLY));
        view.layout(0,0,1080,950);view.draw(canvas);
        File file=new File(context.getCacheDir(),"haiagaru-x-card-sample.png");
        try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
        bitmap.recycle();return file.getAbsolutePath();
    }
}
