import app.morphe.extension.chmate.TalkPostCompatibility;
public final class VerifyTalk242 {
    public static String ordinary(String input) { return input; }
    private static void outside() {
        Object[] state = {new int[]{9}, new String[]{"untouched"}, new int[]{1}, new int[]{0}};
        if (TalkPostCompatibility.normalizeIntegrityState242(state) != state
                || ((int[]) state[2])[0] != 1 || ((int[]) state[3])[0] != 0)
            throw new AssertionError("scope leaked");
    }
    private static void arithmetic(int cpu) {
        int seed = 5704;
        int v = seed + (-628195952 + ((~(-570737027 | cpu) | ~(-339804745 | cpu)) * 69)
                + ((~(cpu | -500767321) | (~(-731699603 | cpu) | 160962576)) * -69)) - 919355911;
        int a = (v << 13) ^ v;
        int b = a ^ (a >>> 17);
        int c = b ^ (b << 5);
        int d = c * c, e = -(2048028980 * c);
        int f = (((d | e) << 1) - (d ^ e)) - ~(-(c * 1188405564)) - 1;
        int g = (f & 1528101952) + (1528101952 | f);
        int h = g >> 21;
        int i = ((h ^ -4095) + ((h & -4095) << 1)) / 2048;
        int j = (i ^ 1) + ((i & 1) << 1);
        int k = ((g | j) << 1) - (j ^ g);
        int l = g >> 23;
        int m = (((l | -1023) << 1) - (l ^ -1023)) / 512;
        int n = -((((m | 1) << 1) - (m ^ 1)) ^ k) + 2;
        int p = n >> 17;
        int q = ((-65535 & p) + (p | -65535)) / 32768;
        int r = (q & 1) + (q | 1);
        int divisor = (n & -(((r | 1) << 1) - (r ^ 1))) * 797;
        if (divisor != 1594 || 19128 / divisor != 12) throw new AssertionError("seed arithmetic");
    }
    public static void main(String[] args) throws Throwable {
        outside();
        var ordinary = VerifyTalk242.class.getMethod("ordinary", String.class);
        if (!"untouched".equals(TalkPostCompatibility.invoke(ordinary, null, new Object[]{"untouched"})))
            throw new AssertionError("non-Talk single String reflection changed");
        var digest = o.success.class.getMethod("d", boolean.class);
        for (int i = 0; i < 10; i++) {
            o.GNk4.e = 42;
            TalkPostCompatibility.invoke(digest, null, new Object[]{false});
            outside();
        }
        try {
            TalkPostCompatibility.invoke(digest, null, new Object[]{true});
            throw new AssertionError("swallowed exception");
        } catch (RuntimeException failure) {
            if (failure != o.success.FAILURE) throw new AssertionError("wrapped exception");
        }
        outside();
        var auth = o.success.class.getMethod("b", o.zzcvr.class, String.class, String.class, long.class);
        Object[] original = {new o.zzcvr(), "", "", 1790000000L};
        if (!Long.valueOf(1790000000000L).equals(TalkPostCompatibility.invoke(auth, null, original))
                || !Long.valueOf(1790000000L).equals(original[3])) throw new AssertionError("timestamp conversion");
        try {
            TalkPostCompatibility.invoke(auth, null, new Object[]{new o.zzcvr(), "", "", Long.MAX_VALUE});
            throw new AssertionError("timestamp overflow accepted");
        } catch (ArithmeticException expected) { }
        outside();
        var random = new java.util.Random(242);
        arithmetic(0); arithmetic(Integer.MAX_VALUE); arithmetic(Integer.MIN_VALUE);
        for (int i = 0; i < 100000; i++) arithmetic(random.nextInt());
        System.out.println("PASS: 242 cache, scope cleanup, timestamp, exception and 100003 arithmetic cases");
    }
}
