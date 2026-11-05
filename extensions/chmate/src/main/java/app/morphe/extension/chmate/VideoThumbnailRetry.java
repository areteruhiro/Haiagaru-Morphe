package app.morphe.extension.chmate;

import android.view.*;import android.widget.*;import android.content.*;

/** Retry is confined to failed previews; ordinary thumbnail binding has no posted work. */
public final class VideoThumbnailRetry {
    private static final int TAG=0x7e0b0022;
    public static void bind(Object object,String poster){
        if(!(object instanceof View))return;
        View view=(View)object;Object previous=view.getTag(TAG);
        if(poster==null||!poster.contains("/video-error-")||!FeatureSwitches.enabled("videoFrameThumbnailsEnabled",true)){
            if(previous instanceof Slot)((Slot)previous).close();return;
        }
        if(previous instanceof Slot&&poster.equals(((Slot)previous).poster)){((Slot)previous).update();return;}
        if(previous instanceof Slot)((Slot)previous).close();
        String source=XMediaAttachments.sourceVideo(poster);if(source==null)return;
        Slot slot=new Slot(view,poster,source);view.setTag(TAG,slot);
        view.addOnAttachStateChangeListener(slot);
        view.post(()->{if(view.getTag(TAG)==slot){slot.attach();slot.update();}});
    }
    private static final class Slot implements View.OnAttachStateChangeListener,ViewTreeObserver.OnPreDrawListener {
        final View view;final String poster,source;Button button;LinearLayout actions;FrameLayout host;
        final android.graphics.Rect visible=new android.graphics.Rect();final int[] location=new int[2],origin=new int[2];
        Slot(View view,String poster,String source){this.view=view;this.poster=poster;this.source=source;}
        void attach(){
            if(actions!=null||!view.isAttachedToWindow()||!(view.getRootView() instanceof FrameLayout))return;
            host=(FrameLayout)view.getRootView();
            actions=new LinearLayout(host.getContext());actions.setOrientation(LinearLayout.HORIZONTAL);
            button=button("再取得","サムネイルを再取得");
            Button play=button("再生","サムネイルを使わずに動画を再生");
            actions.addView(button,new LinearLayout.LayoutParams(0,-1,1));actions.addView(play,new LinearLayout.LayoutParams(0,-1,1));
            int height=Math.round(36*view.getResources().getDisplayMetrics().density);
            host.addView(actions,new FrameLayout.LayoutParams(Math.max(1,view.getWidth()),height,Gravity.TOP|Gravity.LEFT));
            host.getViewTreeObserver().addOnPreDrawListener(this);position();
            button.setOnClickListener(v->{if(VideoFrameThumbnail.retry(source)){update();view.postDelayed(this::poll,500);}else if(!VideoFrameThumbnail.running(source))Toast.makeText(view.getContext(),"動画サムネイルの取得設定を確認してください",Toast.LENGTH_SHORT).show();});
            play.setOnClickListener(v->{try{view.getContext().startActivity(playbackIntent(view.getContext(),source));}catch(RuntimeException unavailable){Toast.makeText(view.getContext(),"動画を開けませんでした",Toast.LENGTH_SHORT).show();}});
        }
        Button button(String text,String description){Button button=new Button(host.getContext());button.setText(text);button.setContentDescription(description);button.setTextSize(12);button.setAllCaps(false);button.setMinHeight(0);button.setMinimumHeight(0);button.setMinWidth(0);button.setMinimumWidth(0);button.setPadding(4,0,4,0);return button;}
        void update(){if(button!=null){boolean running=VideoFrameThumbnail.running(source);button.setText(running?"再取得中…":"再取得");button.setEnabled(!running);}}
        void poll(){if(view.getTag(TAG)!=this)return;update();if(VideoFrameThumbnail.running(source)&&view.isAttachedToWindow())view.postDelayed(this::poll,500);}
        void position(){
            if(host==null||actions==null)return;
            if(!view.isAttachedToWindow()||!view.isShown()||view.getWidth()<=0||!view.getGlobalVisibleRect(visible)){actions.setVisibility(View.GONE);return;}
            view.getLocationOnScreen(location);host.getLocationOnScreen(origin);int height=actions.getLayoutParams().height;
            int top=location[1]+view.getHeight()-height;
            if(top<visible.top||top+height>visible.bottom){actions.setVisibility(View.GONE);return;}
            ViewGroup.LayoutParams params=actions.getLayoutParams();if(params.width!=view.getWidth()){params.width=view.getWidth();actions.setLayoutParams(params);}
            actions.setX(location[0]-origin[0]);actions.setY(top-origin[1]);actions.setVisibility(View.VISIBLE);
        }
        @Override public boolean onPreDraw(){position();return true;}
        @Override public void onViewAttachedToWindow(View v){view.post(()->{if(view.getTag(TAG)==this){attach();update();}});}
        @Override public void onViewDetachedFromWindow(View v){
            // Android is iterating root children here; removing a sibling during
            // dispatchDetachedFromWindow can invalidate that traversal.
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->{
                detach();if(view.isAttachedToWindow()&&view.getTag(TAG)==this){attach();update();}
            });
        }
        void detach(){if(host!=null){if(host.getViewTreeObserver().isAlive())host.getViewTreeObserver().removeOnPreDrawListener(this);if(actions!=null)host.removeView(actions);}actions=null;button=null;host=null;}
        void close(){detach();view.removeOnAttachStateChangeListener(this);if(view.getTag(TAG)==this)view.setTag(TAG,null);}
    }
    static Intent playbackIntent(android.content.Context context,String source){
        java.util.ArrayList<String> links=new java.util.ArrayList<>();links.add(source);
        Intent intent=new Intent().setClassName(context,"jp.syoboi.a2chMate.activity.ImagesViewer2Activity")
                .putStringArrayListExtra("links",links).putExtra("index",0).putExtra("forwardTabletHome",false);
        MediaViewerNavigation.prepareNativeIntent(intent,context);
        if(NativeToolTabs.activity(context)==null)intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
    private VideoThumbnailRetry(){}
}
