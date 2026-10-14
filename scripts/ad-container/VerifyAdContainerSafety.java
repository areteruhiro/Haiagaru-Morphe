import app.morphe.extension.chmate.AdContainerSafety;
public class VerifyAdContainerSafety {
    static class Host extends androidx.fragment.app.FragmentContainerView {}
    static class AdSlot {}
    public static void main(String[] args) {
        if (!AdContainerSafety.isScreenHost(androidx.fragment.app.FragmentContainerView.class, -1)
                || !AdContainerSafety.isScreenHost(Host.class, -1)
                || !AdContainerSafety.isScreenHost(Object.class, 0x01020002))
            throw new AssertionError("screen host is not protected");
        if (AdContainerSafety.isScreenHost(AdSlot.class, -1)
                || AdContainerSafety.isScreenHost(Object.class, 42))
            throw new AssertionError("ordinary ad slot is protected");
        System.out.println("PASS: fragment hosts/subclasses and content root protected; ad slots unchanged");
    }
}
