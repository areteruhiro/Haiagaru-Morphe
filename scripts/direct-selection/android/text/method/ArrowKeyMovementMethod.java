package android.text.method;
public class ArrowKeyMovementMethod implements MovementMethod {
    private static final ArrowKeyMovementMethod INSTANCE=new ArrowKeyMovementMethod();
    public static ArrowKeyMovementMethod getInstance(){return INSTANCE;}
}
