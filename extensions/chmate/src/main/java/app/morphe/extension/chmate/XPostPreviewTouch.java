package app.morphe.extension.chmate;

import android.content.Intent;
import android.net.Uri;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.TextView;
import android.widget.Toast;
import java.util.WeakHashMap;

/** Intercepts only our appended link; stock links, selection and gestures stay native. */
public final class XPostPreviewTouch {
    private static final WeakHashMap<TextView, Press> PRESSES = new WeakHashMap<>();
    static class PostLink extends URLSpan {
        PostLink(String url) { super(url); }
        @Override public void onClick(View view) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getURL()));
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                view.getContext().startActivity(intent);
            } catch (android.content.ActivityNotFoundException error) {
                Toast.makeText(view.getContext(), "投稿を開けるアプリがありません", Toast.LENGTH_SHORT).show();
            }
        }
    }
    static final class ToggleLink extends PostLink {
        final String id;
        ToggleLink(String id) { super("https://x.com/i/status/" + id); this.id=id; }
        @Override public void onClick(View view) {
            if(view instanceof TextView)XPostPreview.toggle((TextView)view,id);
        }
    }
    private static final class Press {
        final PostLink link;
        final float x, y;
        boolean moved;
        Press(PostLink link, MotionEvent event) { this.link=link; x=event.getX(); y=event.getY(); }
    }
    public static boolean onTouch(Object object, MotionEvent event) {
        if (!(object instanceof TextView)) return false;
        TextView view=(TextView)object;
        int action=event.getActionMasked();
        if (action==MotionEvent.ACTION_DOWN) {
            PRESSES.remove(view);
            PostLink link=hit(view,event);
            if(link==null)return false;
            PRESSES.put(view,new Press(link,event));
            return true;
        }
        Press press=PRESSES.get(view);
        if(press==null)return false;
        int slop=ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
        if(Math.abs(event.getX()-press.x)>slop || Math.abs(event.getY()-press.y)>slop
                || event.getPointerCount()!=1)press.moved=true;
        if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) {
            PRESSES.remove(view);
            if(action==MotionEvent.ACTION_UP && !press.moved && Haiagaru.xPostPreviewEnabled()
                    && event.getEventTime()-event.getDownTime()<ViewConfiguration.getLongPressTimeout()
                    && hit(view,event)==press.link)press.link.onClick(view);
        }
        return true;
    }
    private static PostLink hit(TextView view, MotionEvent event) {
        CharSequence text=view.getText(); Layout layout=view.getLayout();
        if(!(text instanceof Spanned)||layout==null)return null;
        float x=event.getX()-view.getTotalPaddingLeft()+view.getScrollX();
        float y=event.getY()-view.getTotalPaddingTop()+view.getScrollY();
        if(y<0||y>=layout.getHeight())return null;
        int line=layout.getLineForVertical((int)y);
        if(x<layout.getLineLeft(line)||x>layout.getLineRight(line))return null;
        int offset=layout.getOffsetForHorizontal(line,x);
        Spanned spans=(Spanned)text;
        for(PostLink link:spans.getSpans(offset,offset,PostLink.class))
            if(offset>=spans.getSpanStart(link)&&offset<spans.getSpanEnd(link))return link;
        return null;
    }
}
