package android.view;
public class View {
    public ViewParent parent;
    public ViewParent getParent() { return parent; }
    public interface OnLongClickListener { boolean onLongClick(View view); }
}
