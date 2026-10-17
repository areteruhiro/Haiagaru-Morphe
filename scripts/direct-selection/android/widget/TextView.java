package android.widget;
import android.text.method.MovementMethod;
import android.view.MotionEvent;
import android.view.ActionMode;
public class TextView extends android.view.View {
    boolean selectable,focusable,touchFocusable,clickable,longClickable;
    MovementMethod movement;
    ActionMode.Callback selectionCallback;
    public boolean selected; public int frameworkEvents,cancels,nativeDown,nativeUp;
    public Object getContext(){return null;}
    public boolean isTextSelectable(){return selectable;}
    public void setTextIsSelectable(boolean value){selectable=value;focusable=value;touchFocusable=value;clickable=value;longClickable=value;}
    public boolean isFocusable(){return focusable;} public void setFocusable(boolean value){focusable=value;}
    public boolean isFocusableInTouchMode(){return touchFocusable;} public void setFocusableInTouchMode(boolean value){touchFocusable=value;}
    public boolean isClickable(){return clickable;} public void setClickable(boolean value){clickable=value;}
    public boolean isLongClickable(){return longClickable;} public void setLongClickable(boolean value){longClickable=value;}
    public MovementMethod getMovementMethod(){return movement;} public void setMovementMethod(MovementMethod value){movement=value;}
    public ActionMode.Callback getCustomSelectionActionModeCallback(){return selectionCallback;}
    public void setCustomSelectionActionModeCallback(ActionMode.Callback value){selectionCallback=value;}
    public boolean hasSelection(){return selected;}
    public void clearFocus(){selected=false;}
    public boolean onTouchEvent(MotionEvent event){frameworkEvents++;if(event.getActionMasked()==3)cancels++;return true;}
    public boolean haiagaruDirectSelectionNativeDispatch(MotionEvent event){
        if(event.getActionMasked()==0)nativeDown++;if(event.getActionMasked()==1)nativeUp++;return true;
    }
}
