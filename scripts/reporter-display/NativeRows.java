package o;

/** Native field layouts, kept aligned with the bytecode hook verifier. */
public final class NativeRows {
    public static Object adapter(int version, Object board) {
        return version == 191 ? new m8a(board)
                : version == 226 ? new getWriteBytesTotal(board) : new zzavb(board);
    }
    public static Object subject(int version, long thread) {
        return version == 191 ? new MaxAdViewImplExternalSyntheticLambda1(thread)
                : version == 226 ? new CertificateChainCleaner(thread) : new setSkipText(thread);
    }
}
class m8a { private final Object r; m8a(Object board) { r = board; } }
class getWriteBytesTotal { private final Object g; getWriteBytesTotal(Object board) { g = board; } }
class zzavb { private final Object c; zzavb(Object board) { c = board; } }
class MaxAdViewImplExternalSyntheticLambda1 {
    private final long e;
    MaxAdViewImplExternalSyntheticLambda1(long thread) { e = thread; }
}
class CertificateChainCleaner {
    private final long b;
    CertificateChainCleaner(long thread) { b = thread; }
}
class setSkipText {
    private final long d;
    setSkipText(long thread) { d = thread; }
}
