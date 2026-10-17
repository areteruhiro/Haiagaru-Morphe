package android.view;
public final class ViewConfiguration {
    public static ViewConfiguration get(Object ignored){return new ViewConfiguration();}
    public int getScaledTouchSlop(){return 8;}
    public static int getLongPressTimeout(){return 500;}
}
