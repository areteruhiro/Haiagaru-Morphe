package android.view;
public final class MotionEvent {
    public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3;
    int action; final long time; final float x,y; public boolean recycled;
    public MotionEvent(int action,long time,float x,float y){this.action=action;this.time=time;this.x=x;this.y=y;}
    public static MotionEvent obtain(MotionEvent source){return new MotionEvent(source.action,source.time,source.x,source.y);}
    public int getActionMasked(){return action;}
    public void setAction(int value){action=value;}
    public long getEventTime(){return time;}
    public float getX(){return x;} public float getY(){return y;}
    public int getPointerCount(){return 1;}
    public void recycle(){recycled=true;}
}
