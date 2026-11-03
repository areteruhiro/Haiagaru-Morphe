package app.morphe.extension.chmate;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** An entry in the settings window, without detaching the native/Compose content. */
final class InlineSettingsEntry {
    static final String TAG="haiagaru.settings.inline";
    static void attach(Activity activity) {
        View view=activity.findViewById(android.R.id.content);
        if(!(view instanceof FrameLayout))return;
        FrameLayout content=(FrameLayout)view;
        if(content.findViewWithTag(TAG)!=null)return;
        if(content.getChildCount()==0)return;
        java.util.List<View> originals=new java.util.ArrayList<>();
        java.util.List<Integer> margins=new java.util.ArrayList<>();
        for(int i=0;i<content.getChildCount();i++){
            View child=content.getChildAt(i);
            if(!(child.getLayoutParams() instanceof FrameLayout.LayoutParams))return;
            originals.add(child);margins.add(((FrameLayout.LayoutParams)child.getLayoutParams()).topMargin);
        }
        int padding=dp(activity,16),height=dp(activity,72);
        LinearLayout entry=new LinearLayout(activity);entry.setTag(TAG);
        entry.setOrientation(LinearLayout.VERTICAL);entry.setGravity(Gravity.CENTER_VERTICAL);
        entry.setPadding(padding,dp(activity,8),padding,dp(activity,8));
        entry.setMinimumHeight(height);entry.setClickable(true);entry.setFocusable(true);
        android.util.TypedValue attr=new android.util.TypedValue();
        if(activity.getTheme().resolveAttribute(android.R.attr.colorBackground,attr,true))entry.setBackgroundColor(attr.data);
        if(android.os.Build.VERSION.SDK_INT>=23&&activity.getTheme().resolveAttribute(android.R.attr.selectableItemBackground,attr,true)&&attr.resourceId!=0)
            entry.setForeground(activity.getDrawable(attr.resourceId));
        TextView title=new TextView(activity);title.setText("Haiagaru設定");title.setTextSize(17);
        TextView summary=new TextView(activity);summary.setText("追加機能・表示・通知などの設定");summary.setTextSize(13);
        entry.addView(title);entry.addView(summary);entry.setElevation(dp(activity,2));
        entry.setOnClickListener(v->Haiagaru.showSettingsDialog(activity));
        java.util.function.IntConsumer reserve=top->{
            for(int i=0;i<originals.size();i++){
                View child=originals.get(i);
                FrameLayout.LayoutParams params=(FrameLayout.LayoutParams)child.getLayoutParams();
                int margin=margins.get(i)+top;
                if(params.topMargin!=margin){params.topMargin=margin;child.setLayoutParams(params);}
            }
        };
        reserve.accept(height);
        content.addView(entry,new FrameLayout.LayoutParams(-1,height,Gravity.TOP));
        entry.setOnApplyWindowInsetsListener((v,insets)->{
            int top=android.os.Build.VERSION.SDK_INT>=30?insets.getInsets(android.view.WindowInsets.Type.systemBars()).top:insets.getStableInsetTop();
            entry.setPadding(padding,dp(activity,8)+top,padding,dp(activity,8));
            ViewGroup.LayoutParams params=entry.getLayoutParams();params.height=height+top;entry.setLayoutParams(params);
            reserve.accept(height+top);return insets;
        });
        entry.requestApplyInsets();
    }
    private static int dp(Activity activity,int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    private InlineSettingsEntry(){}
}
