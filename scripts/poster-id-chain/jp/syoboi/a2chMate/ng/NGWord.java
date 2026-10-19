package jp.syoboi.a2chMate.ng;
public final class NGWord {
    public static final class Item {
        public short flags;
        public final String word;
        public Item(String word, int flags) { this.word = word; this.flags = (short) flags; }
        @Override public String toString() { return word; }
    }
}
