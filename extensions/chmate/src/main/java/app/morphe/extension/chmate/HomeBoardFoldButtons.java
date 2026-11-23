package app.morphe.extension.chmate;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Adds explicit fold/unfold controls to board headings while leaving the heading tap intact. */
final class HomeBoardFoldButtons {
    private static final int TAG=0x48690141;
    static final String VISIBLE="homeBoardFoldButtons";
    static final String TITLE_CONTROL="homeBoardFoldToggleInTitle";
    private HomeBoardFoldButtons(){}
    static void menu(Object fragment,android.view.Menu menu){
        if(menu==null)return;
        android.content.Context context=Haiagaru.applicationContextForExtension();if(context==null)return;
        android.view.MenuItem item=menu.findItem(TAG);
        if(item==null)item=menu.add(0,TAG,0,"板見出しの開閉ボタンを表示");
        item.setCheckable(true).setChecked(SettingsProfiles.prefs(context).getBoolean(VISIBLE,true))
            .setVisible(boardOrder(fragment)&&!SettingsProfiles.prefs(context).getBoolean(TITLE_CONTROL,true)).setOnMenuItemClickListener(v->{
                SettingsProfiles.prefs(context).edit().putBoolean(VISIBLE,!v.isChecked()).apply();
                refresh(fragment);android.app.Activity a=Haiagaru.toolbarActivity(fragment);if(a!=null)a.invalidateOptionsMenu();return true;
            });
    }
    private static boolean boardOrder(Object fragment){
        if(HomeThreadOrder.mode(fragment)!=102)return false;
        List<?> rows=HomeThreadOrder.presented(102);if(rows==null)return false;
        for(Object row:rows)if(HomeThreadOrder.boardSection(row))return true;
        return false;
    }

    static void attach(Object fragment){
        if(fragment==null)return;
        try{
            View root=(View)fragment.getClass().getMethod("getView").invoke(fragment);
            View list=FilterPosition.findList(root);
            if(root==null||list==null)return;
            Object existing=list.getTag(TAG);
            if(existing instanceof Binding){((Binding)existing).bind();return;}
            Binding binding=new Binding(fragment,root,list);list.setTag(TAG,binding);
            binding.install();binding.bind();
        }catch(Exception error){android.util.Log.w("Haiagaru","Board fold controls unavailable",error);}
    }
    static void refresh(Object fragment){
        if(fragment==null)return;
        try{
            View root=(View)fragment.getClass().getMethod("getView").invoke(fragment);
            View list=FilterPosition.findList(root);if(list==null)return;
            Object value=list.getTag(TAG);if(value instanceof Binding)((Binding)value).bind();
        }catch(Exception ignored){}
    }

    private static final class Binding implements ViewTreeObserver.OnGlobalLayoutListener,
            View.OnAttachStateChangeListener {
        final WeakReference<Object> fragment;
        final WeakReference<View> root,list;
        final Map<TextView,Bound> bound=new IdentityHashMap<>();
        Button titleToggle;
        View title;
        LinearLayout.LayoutParams titleParams;
        List<?> checkedRows;int checkedSize=-1;boolean hasBoardSections;
        boolean observing;
        Binding(Object f,View r,View l){fragment=new WeakReference<>(f);root=new WeakReference<>(r);list=new WeakReference<>(l);}
        void install(){
            View value=list.get();if(value==null)return;
            value.addOnAttachStateChangeListener(this);
            registerObservers(value);
        }
        private void registerObservers(View value){
            if(observing)return;observing=true;
            ViewTreeObserver observer=value.getViewTreeObserver();if(observer.isAlive())observer.addOnGlobalLayoutListener(this);
        }
        @Override public void onGlobalLayout(){bind();}
        void bind(){
            View value=list.get();Object owner=fragment.get();
            if(value==null||owner==null)return;
            if(HomeThreadOrder.mode(owner)!=102){showTitleToggle(false);clearRows();return;}
            List<?> rows=HomeThreadOrder.presented(102);if(rows==null)return;
            if(rows!=checkedRows||rows.size()!=checkedSize){checkedRows=rows;checkedSize=rows.size();hasBoardSections=false;
                for(Object row:rows)if(HomeThreadOrder.boardSection(row)){hasBoardSections=true;break;}}
            bindTitleToggle(owner,hasBoardSections);
            java.util.Set<TextView> visible=java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            if(value instanceof ListView){
                ListView lv=(ListView)value;int first=lv.getFirstVisiblePosition();
                for(int i=0;i<lv.getChildCount();i++){
                    int position=first+i;if(position>=0&&position<lv.getCount())
                        bindRow(owner,lv.getChildAt(i),lv.getItemAtPosition(position),visible);
                }
            }else if(value instanceof ViewGroup){
                ViewGroup group=(ViewGroup)value;
                try{
                    Method positionMethod=value.getClass().getMethod("getChildAdapterPosition",View.class);
                    for(int i=0;i<group.getChildCount();i++){
                        View child=group.getChildAt(i);int position=((Number)positionMethod.invoke(value,child)).intValue();
                        if(position>=0&&position<rows.size())bindRow(owner,child,rows.get(position),visible);
                    }
                }catch(ReflectiveOperationException ignored){}
            }
            java.util.Iterator<Map.Entry<TextView,Bound>> it=bound.entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<TextView,Bound> item=it.next();
                if(!visible.contains(item.getKey())){item.getValue().restore(item.getKey());it.remove();}
            }
        }
        private void clearRows(){
            for(Map.Entry<TextView,Bound> entry:bound.entrySet())entry.getValue().restore(entry.getKey());bound.clear();
        }
        private void bindTitleToggle(Object owner,boolean visible){
            View container=root.get();if(container==null)return;
            visible=visible&&SettingsProfiles.prefs(container.getContext()).getBoolean(TITLE_CONTROL,true);
            if(titleToggle==null&&visible){
                title=findTopTitle(container,list.get(),container);
                if(title==null||!(title.getParent() instanceof LinearLayout)||!(title.getLayoutParams() instanceof LinearLayout.LayoutParams))return;
                LinearLayout parent=(LinearLayout)title.getParent();
                titleParams=new LinearLayout.LayoutParams((LinearLayout.LayoutParams)title.getLayoutParams());
                titleToggle=new Button(container.getContext());titleToggle.setTag("haiagaru.home.board-fold-toggle");
                titleToggle.setTextSize(12);titleToggle.setAllCaps(false);titleToggle.setMinWidth(0);titleToggle.setMinimumWidth(0);
                int inset=Math.round(6*container.getResources().getDisplayMetrics().density);titleToggle.setPadding(inset,0,inset,0);
                parent.addView(titleToggle,new LinearLayout.LayoutParams(Math.round(88*container.getResources().getDisplayMetrics().density),-1));
                titleToggle.setOnClickListener(v->{
                    android.content.Context context=Haiagaru.applicationContextForExtension();if(context==null)return;
                    boolean show=SettingsProfiles.prefs(context).getBoolean(VISIBLE,true);
                    SettingsProfiles.prefs(context).edit().putBoolean(VISIBLE,!show).apply();bind();
                    android.app.Activity activity=Haiagaru.toolbarActivity(owner);if(activity!=null)activity.invalidateOptionsMenu();
                });
            }
            showTitleToggle(visible);
            if(titleToggle!=null){boolean show=SettingsProfiles.prefs(container.getContext()).getBoolean(VISIBLE,true);
                String text=show?"↓↑ 非表示":"↓↑ 表示";if(!text.contentEquals(titleToggle.getText()))titleToggle.setText(text);
                titleToggle.setContentDescription(show?"板見出しの開閉ボタンを非表示":"板見出しの開閉ボタンを表示");}
        }
        private void showTitleToggle(boolean visible){
            if(titleToggle==null||titleParams==null)return;
            int visibility=visible?View.VISIBLE:View.GONE;
            if(titleToggle.getVisibility()==visibility&&title.getLayoutParams().width==(visible?0:titleParams.width))return;
            titleToggle.setVisibility(visibility);
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(titleParams);
            if(visible){params.width=0;params.weight=1;}
            title.setLayoutParams(params);
        }
        private void bindRow(Object owner,View view,Object row,java.util.Set<TextView> visible){
            android.content.Context context=Haiagaru.applicationContextForExtension();
            if(context==null||!SettingsProfiles.prefs(context).getBoolean(VISIBLE,true)||!HomeThreadOrder.boardSection(row))return;
            TextView title=firstText(view);if(title==null)return;
            visible.add(title);Bound state=bound.get(title);
            if(state!=null&&state.row==row){state.update(owner,row);return;}
            if(state!=null)state.restore(title);
            state=new Bound(owner,row,title);bound.put(title,state);state.install(title);
        }
        @Override public void onViewAttachedToWindow(View v){registerObservers(v);bind();}
        @Override public void onViewDetachedFromWindow(View v){
            View value=list.get();if(value!=null){ViewTreeObserver observer=value.getViewTreeObserver();if(observer.isAlive())observer.removeOnGlobalLayoutListener(this);
                }observing=false;
            for(Map.Entry<TextView,Bound> entry:bound.entrySet())entry.getValue().restore(entry.getKey());bound.clear();
        }
    }

    private static final class Bound {
        final Object row;
        final WeakReference<Object> fragment;
        final Drawable[] oldDrawables;
        final int oldPaddingEnd;
        final int oldMinHeight;
        final int color;
        final Touch touch=new Touch(this);
        Buttons drawable;
        Bound(Object owner,Object item,TextView title){row=item;fragment=new WeakReference<>(owner);oldDrawables=title.getCompoundDrawablesRelative();oldPaddingEnd=title.getPaddingEnd();oldMinHeight=title.getMinimumHeight();color=title.getCurrentTextColor();}
        void install(TextView title){
            if(oldDrawables[2]!=null)return;
            drawable=new Buttons(title.getResources().getDisplayMetrics().density);
            drawable.setBounds(0,0,drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight());
            title.setMinimumHeight(Math.max(oldMinHeight,drawable.getIntrinsicHeight()));
            title.setCompoundDrawablesRelative(oldDrawables[0],oldDrawables[1],drawable,oldDrawables[3]);
            title.setPaddingRelative(title.getPaddingStart(),title.getPaddingTop(),oldPaddingEnd,title.getPaddingBottom());
            title.setOnTouchListener(touch);update(fragment.get(),row);
        }
        void update(Object owner,Object item){
            if(drawable==null)return;
            boolean folded=HomeThreadOrder.boardSectionFolded(item,102);
            drawable.setFolded(folded);
            drawable.setColor(color);
            android.content.Context context=Haiagaru.applicationContextForExtension();
            drawable.setEnabled(context!=null&&SettingsProfiles.prefs(context).getBoolean(HomeThreadOrder.FOLD_ON_TAP,true));
        }
        void restore(TextView title){
            if(drawable==null)return;
            Drawable[] now=title.getCompoundDrawablesRelative();
            if(now[2]==drawable){title.setCompoundDrawablesRelative(oldDrawables[0],oldDrawables[1],oldDrawables[2],oldDrawables[3]);title.setPaddingRelative(title.getPaddingStart(),title.getPaddingTop(),oldPaddingEnd,title.getPaddingBottom());title.setMinimumHeight(oldMinHeight);title.setOnTouchListener(null);}
            drawable=null;
        }
    }
    private static final class Touch implements View.OnTouchListener {
        float downX,downY;boolean tracking,cancelled;
        final Bound bound;Touch(Bound b){bound=b;}
        @Override public boolean onTouch(View view,MotionEvent event){
            if(!(view instanceof TextView)||bound.drawable==null||!bound.drawable.enabled)return false;
            TextView text=(TextView)view;int width=bound.drawable.getIntrinsicWidth();
            float start=text.getWidth()-text.getPaddingRight()-width;
            if(event.getActionMasked()==MotionEvent.ACTION_DOWN){tracking=event.getX()>=start;cancelled=false;downX=event.getX();downY=event.getY();}
            if(!tracking)return false;
            int slop=android.view.ViewConfiguration.get(view.getContext()).getScaledTouchSlop();
            if(Math.abs(event.getX()-downX)>slop||Math.abs(event.getY()-downY)>slop)cancelled=true;
            if(event.getActionMasked()==MotionEvent.ACTION_UP){
                tracking=false;Object owner=bound.fragment.get();if(owner!=null&&!cancelled){
                    HomeThreadOrder.setBoardSectionFolded(owner,bound.row,102,!HomeThreadOrder.boardSectionFolded(bound.row,102));}
            }
            if(event.getActionMasked()==MotionEvent.ACTION_CANCEL){tracking=false;return true;}
            return event.getActionMasked()==MotionEvent.ACTION_DOWN||event.getActionMasked()==MotionEvent.ACTION_MOVE
                ||event.getActionMasked()==MotionEvent.ACTION_UP;
        }
    }
    private static final class Buttons extends Drawable {
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final float density;boolean folded;int color=Color.BLACK;
        boolean enabled=true;
        Buttons(float d){density=d;}
        void setFolded(boolean value){if(folded!=value){folded=value;invalidateSelf();}}
        void setEnabled(boolean value){if(enabled!=value){enabled=value;invalidateSelf();}}
        void setColor(int value){if(color!=value){color=value;invalidateSelf();}}
        @Override public void draw(Canvas canvas){
            if(!enabled)return;
            android.graphics.Rect b=getBounds();paint.setStyle(Paint.Style.STROKE);paint.setColor(color);
            paint.setStrokeWidth(2*density);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
            float x=b.centerX(),y=b.centerY(),s=6*density;
            canvas.drawRect(x-14*density,y-14*density,x+14*density,y+14*density,paint);
            Path mark=new Path();
            if(folded){mark.moveTo(x-s,y-3*density);mark.lineTo(x,y+3*density);mark.lineTo(x+s,y-3*density);}
            else {for(int i=0;i<2;i++){float cy=y+(i==0?-4:4)*density;mark.moveTo(x-s,cy+3*density);mark.lineTo(x,cy-3*density);mark.lineTo(x+s,cy+3*density);}}
            canvas.drawPath(mark,paint);
        }
        private void drawArrow(Canvas canvas,float x,float y,boolean down){
            Path path=new Path();float s=7*density;
            if(down){path.moveTo(x,y-s);path.lineTo(x,y+s);path.moveTo(x-s,y);path.lineTo(x,y+s);path.lineTo(x+s,y);}
            else {path.moveTo(x,y+s);path.lineTo(x,y-s);path.moveTo(x-s,y);path.lineTo(x,y-s);path.lineTo(x+s,y);}
            canvas.drawPath(path,paint);
        }
        @Override public int getIntrinsicWidth(){return Math.round(40*density);}
        @Override public int getIntrinsicHeight(){return Math.round(40*density);}
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
        @Override public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private static View findTopTitle(View node,View list,View root){
        if(node==list)return null;
        if(node instanceof LinearLayout){
            LinearLayout group=(LinearLayout)node;int[] location=new int[2],origin=new int[2];node.getLocationOnScreen(location);root.getLocationOnScreen(origin);
            float density=node.getResources().getDisplayMetrics().density;
            if(group.getOrientation()==LinearLayout.HORIZONTAL&&Math.abs(location[1]-origin[1])<2*density
                    &&node.getHeight()>=28*density&&node.getHeight()<=72*density){
                for(int i=0;i<group.getChildCount();i++){View child=group.getChildAt(i);
                    if(child.getWidth()>root.getWidth()/2&&!(child instanceof ViewGroup)&&!(child instanceof TextView))return child;
                }
            }
        }
        if(node instanceof ViewGroup){ViewGroup group=(ViewGroup)node;for(int i=0;i<group.getChildCount();i++){View found=findTopTitle(group.getChildAt(i),list,root);if(found!=null)return found;}}
        return null;
    }
    private static TextView firstText(View view){
        if(view instanceof TextView){TextView text=(TextView)view;return text.length()>0?text:null;}
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){TextView found=firstText(group.getChildAt(i));if(found!=null)return found;}}
        return null;
    }
}
