package app.morphe.extension.chmate;
import android.view.MotionEvent;
import android.view.ActionMode;
import android.view.View;
import android.widget.TextView;
import android.text.method.MovementMethod;

/** Host-side dispatcher/state tests. Not a replacement for Android selection-handle UI QA. */
public final class SelectionTest {
    static int checks;
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    static void touch(TextView view,int action,long time,float x){
        MotionEvent event=new MotionEvent(action,time,x,0);
        check(DirectBodySelection.dispatch(view,event),"consumed");
        check(!event.recycled,"caller retains event ownership");
    }
    public static void main(String[] args){
        check(!DirectBodySelection.prepare(null),"null ignored");
        Haiagaru.enabled=true;
        check(!DirectBodySelection.prepare(new TextView()),"header and ordinary labels excluded");
        View.OnLongClickListener listener=v->true;
        check(DirectBodySelection.longClickListener(null,listener)==listener,"nonbody listener unchanged");
        for(TextView view:new TextView[]{new o.r8lambdaDC40CispcBM3LVwPP01tT_FjLc(),new o.setCornerTopRightRadius(),new o.zzbhp()}){
            Haiagaru.enabled=false;
            check(!DirectBodySelection.prepare(view),"off retains native dispatcher");
            MovementMethod original=new MovementMethod(){};
            view.setMovementMethod(original);view.setClickable(true);
            ActionMode.Callback originalCallback=new ActionMode.Callback(){};
            view.setCustomSelectionActionModeCallback(originalCallback);
            Haiagaru.enabled=true;
            check(DirectBodySelection.prepare(view)&&view.isTextSelectable(),"body selectable");
            View.OnLongClickListener wrapped=DirectBodySelection.longClickListener(view,listener);
            check(!wrapped.onLongClick(view),"native menu listener bypassed while enabled");
            check(DirectBodySelection.longClickListener(view,null)==null,"null listener preserved");
            check(DirectBodySelection.prepare(view),"repeat prepare idempotent");
            check(view.getCustomSelectionActionModeCallback()==null,"native suppressing callback disabled during direct selection");
            view.setMovementMethod(original);view.setLongClickable(false);view.setFocusableInTouchMode(false);
            DirectBodySelection.prepare(view);
            check(view.getMovementMethod() instanceof android.text.method.ArrowKeyMovementMethod
                    &&view.isLongClickable()&&view.isFocusableInTouchMode(),"rebound row restores framework selection state");
            final boolean[] intercept={false};
            view.parent=disallow->intercept[0]=disallow;
            touch(view,0,0,0);
            check(intercept[0],"parent cannot cancel selection start");
            touch(view,1,600,0);
            check(!intercept[0],"parent released after long press up");
            touch(view,0,700,0);touch(view,2,710,30);
            check(!intercept[0],"ordinary scrolling releases parent interception");
            touch(view,3,720,30);
            view.selected=true;touch(view,0,750,0);touch(view,2,760,30);
            check(intercept[0],"selection-handle drag keeps parent from stealing touch");
            touch(view,3,770,30);view.selected=false;
            check(!intercept[0],"cancel releases parent interception");
            view.cancels=0;
            touch(view,0,0,0);touch(view,1,100,0);
            check(view.nativeDown==1&&view.nativeUp==1&&view.cancels==1,"short tap replayed once; framework canceled");
            touch(view,0,1000,0);touch(view,1,1600,0);
            check(view.nativeDown==1,"long press never opens native response menu");
            touch(view,0,2000,0);touch(view,2,2010,30);touch(view,1,2100,30);
            check(view.nativeDown==1,"drag not converted to a link tap");
            view.selected=true;touch(view,0,3000,0);view.selected=false;touch(view,1,3100,0);
            check(view.nativeDown==1,"dismiss selection without opening underlying link");
            touch(view,0,4000,0);touch(view,3,4010,0);
            check(view.nativeDown==1,"cancel does not activate link");
            view.text=">>123 body";
            touch(view,0,5000,2);
            check(!DirectBodySelection.selectionContext(view),"anchor permits native context menu");
            check(wrapped.onLongClick(view),"anchor retains native long click listener");
            touch(view,1,5700,2);
            check(view.nativeDown==2&&view.nativeUp==2,"anchor long press dispatches native down and up");
            check(DirectBodySelection.selectionContext(view),"ordinary selection restored after anchor");
            Haiagaru.anchors=false;
            touch(view,0,6000,2);touch(view,1,6700,2);
            check(view.nativeDown==2,"anchor toggle off retains text selection");
            check(view.longPressRetries>0,"missing long press selection retried");
            Haiagaru.anchors=true;
            view.text="ordinary body";
            Haiagaru.enabled=false;
            check(!DirectBodySelection.prepare(view),"off switches to native");
            check(wrapped.onLongClick(view),"native menu listener restored while disabled");
            check(!view.isTextSelectable()&&!view.isFocusable()&&!view.isFocusableInTouchMode(),"original focus restored");
            check(view.isClickable()&&!view.isLongClickable()&&view.getMovementMethod()==original,"original flags and movement restored");
            check(view.getCustomSelectionActionModeCallback()==originalCallback,"native selection callback restored on opt-out");
            Haiagaru.enabled=true;check(DirectBodySelection.prepare(view),"recycled row can reenable");
            Haiagaru.enabled=false;DirectBodySelection.prepare(view);
        }
        System.out.println("PASS "+checks+" direct selection dispatcher/state checks (host stubs; not UI)");
    }
}
