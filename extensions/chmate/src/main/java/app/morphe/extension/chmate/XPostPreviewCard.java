package app.morphe.extension.chmate;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.LineHeightSpan;
import android.widget.TextView;

/** Native paragraph decoration: no WebView, script, bitmap, extra fetch or retained View. */
final class XPostPreviewCard implements LeadingMarginSpan, LineBackgroundSpan, LineHeightSpan {
    private final float density;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path shape = new Path(), outline = new Path();

    XPostPreviewCard(float density) { this.density = Math.max(.5f, density); }
    static void add(SpannableStringBuilder text, int start, TextView view) {
        float density = view == null ? android.content.res.Resources.getSystem().getDisplayMetrics().density
                : view.getResources().getDisplayMetrics().density;
        text.setSpan(new XPostPreviewCard(density), start, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    @Override public int getLeadingMargin(boolean first) { return Math.round(14 * density); }
    @Override public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top,
            int baseline, int bottom, CharSequence text, int start, int end, boolean first,
            android.text.Layout layout) { }

    private boolean first(CharSequence text, int start) {
        return text instanceof Spanned && start <= ((Spanned) text).getSpanStart(this);
    }
    private boolean last(CharSequence text, int end) {
        return text instanceof Spanned && end >= ((Spanned) text).getSpanEnd(this);
    }
    @Override public void chooseHeight(CharSequence text, int start, int end, int spanstartv,
            int v, Paint.FontMetricsInt fm) {
        int padding = Math.round(10 * density);
        if (first(text,start)) { fm.top -= padding; fm.ascent -= padding; }
        if (last(text,end)) { fm.bottom += padding; fm.descent += padding; }
    }
    @Override public void drawBackground(Canvas canvas, Paint textPaint, int left, int right,
            int top, int baseline, int bottom, CharSequence text, int start, int end, int line) {
        if (right <= left) return;
        int color = textPaint.getColor();
        boolean dark = Color.red(color)*299 + Color.green(color)*587 + Color.blue(color)*114 >= 150000;
        fill.setColor(dark ? 0xff15202b : 0xfff8f9fb);
        border.setColor(dark ? 0xff536471 : 0xffcfd9de);
        border.setStyle(Paint.Style.STROKE); border.setStrokeWidth(Math.max(1,density));
        float l=left+density, r=right-density, t=top, b=bottom;
        float radius=Math.min(8*density,Math.min((r-l)/2,(b-t)/2));
        if (radius < 0) return;
        float rt=first(text,start)?radius:0, rb=last(text,end)?radius:0;
        shape.rewind();
        shape.moveTo(l+rt,t);shape.lineTo(r-rt,t);shape.quadTo(r,t,r,t+rt);
        shape.lineTo(r,b-rb);shape.quadTo(r,b,r-rb,b);shape.lineTo(l+rb,b);
        shape.quadTo(l,b,l,b-rb);shape.lineTo(l,t+rt);shape.quadTo(l,t,l+rt,t);shape.close();
        canvas.drawPath(shape,fill);
        // Only the outer edge: no horizontal border across wrapped text lines.
        outline.rewind();
        if (rt>0) { outline.moveTo(l,t+rt);outline.quadTo(l,t,l+rt,t);outline.lineTo(r-rt,t);outline.quadTo(r,t,r,t+rt); }
        outline.moveTo(l,t+rt);outline.lineTo(l,b-rb);
        outline.moveTo(r,t+rt);outline.lineTo(r,b-rb);
        if (rb>0) { outline.moveTo(l,b-rb);outline.quadTo(l,b,l+rb,b);outline.lineTo(r-rb,b);outline.quadTo(r,b,r,b-rb); }
        canvas.drawPath(outline,border);
    }
}
