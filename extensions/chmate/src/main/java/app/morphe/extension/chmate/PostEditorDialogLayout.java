package app.morphe.extension.chmate;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

/** 242 floating editor: keep the native panel inside the viewport after IME/rotation layout. */
public final class PostEditorDialogLayout {
    public static void fit(ViewGroup root){
        if(root==null||root.getChildCount()!=1||root.getWidth()<=0||root.getHeight()<=0)return;
        Activity activity=NativeToolTabs.activity(root.getContext());
        if(activity==null||!activity.getClass().getName().equals("jp.syoboi.a2chMate.activity.ResEditActivity$Dialog"))return;
        View panel=root.getChildAt(0);
        int left=root.getPaddingLeft(),top=root.getPaddingTop();
        int width=root.getWidth()-left-root.getPaddingRight(),height=root.getHeight()-top-root.getPaddingBottom();
        if(width<=0||height<=0)return;
        if(panel.getLeft()>=left&&panel.getRight()<=left+width&&panel.getTop()>=top&&panel.getBottom()<=top+height
                &&panel.getMeasuredWidth()<=width&&panel.getMeasuredHeight()<=height)return;
        // Re-measure the existing weighted/scrolling form; retain all controls, listeners and draft text.
        panel.measure(View.MeasureSpec.makeMeasureSpec(Math.min(width,Math.max(1,panel.getMeasuredWidth())),View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.AT_MOST));
        int w=panel.getMeasuredWidth(),h=panel.getMeasuredHeight();
        int x=left+(width-w)/2,y=Math.max(top,Math.min(panel.getTop(),top+height-h));
        panel.layout(x,y,x+w,y+h);
    }
    private PostEditorDialogLayout(){}
}
