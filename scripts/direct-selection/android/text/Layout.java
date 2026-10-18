package android.text;
public class Layout {
    public int getHeight(){return 20;}
    public int getLineForVertical(int y){return 0;}
    public float getLineLeft(int line){return 0;}
    public float getLineRight(int line){return 100;}
    public int getOffsetForHorizontal(int line,float x){return (int)x;}
}
