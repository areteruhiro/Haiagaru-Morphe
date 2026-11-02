package app.morphe.extension.chmate;

/** Request-time rotation: no timer, background wakeup, or mutation of an in-flight request. */
final class RandomUserAgentRotation {
    static final String KEY="randomUserAgentEveryFiveSeconds";
    static final long PERIOD_MS=5000;
    private static final String[] AGENTS={
        "Dalvik/2.1.0 (Linux; U; Android 14; Pixel 8 Build/AP2A.240905.003)",
        "Dalvik/2.1.0 (Linux; U; Android 15; Pixel 9 Build/AP4A.250205.002)",
        "Dalvik/2.1.0 (Linux; U; Android 16; Pixel 9 Build/BP2A.250605.031)",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/156.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Android 16; Mobile; rv:146.0) Gecko/146.0 Firefox/146.0"
    };
    private static final java.util.Random RANDOM=new java.util.Random();
    private static long slot=Long.MIN_VALUE;
    private static int selected=-1;
    static synchronized String at(long elapsedMillis){
        long nextSlot=elapsedMillis/PERIOD_MS;
        if(selected<0){selected=RANDOM.nextInt(AGENTS.length);slot=nextSlot;}
        else if(nextSlot!=slot){int next=RANDOM.nextInt(AGENTS.length-1);selected=next>=selected?next+1:next;slot=nextSlot;}
        return AGENTS[selected];
    }
    private RandomUserAgentRotation(){}
}
