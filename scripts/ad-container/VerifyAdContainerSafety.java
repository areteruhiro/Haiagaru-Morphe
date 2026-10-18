import app.morphe.extension.chmate.AdContainerSafety;
public class VerifyAdContainerSafety {
    static class Host extends androidx.fragment.app.FragmentContainerView {}
    static class AdSlot {}
    static class PopupStack { private Object mActivePopup; }
    static class PopupSubclass extends PopupStack {}
    public static void main(String[] args) {
        if (!AdContainerSafety.isScreenHost(androidx.fragment.app.FragmentContainerView.class, -1)
                || !AdContainerSafety.isScreenHost(Host.class, -1)
                || !AdContainerSafety.isScreenHost(Object.class, 0x01020002)
                || !AdContainerSafety.isScreenHost(PopupStack.class, -1)
                || !AdContainerSafety.isScreenHost(PopupSubclass.class, -1))
            throw new AssertionError("screen host is not protected");
        if (AdContainerSafety.isScreenHost(AdSlot.class, -1)
                || AdContainerSafety.isScreenHost(Object.class, 42))
            throw new AssertionError("ordinary ad slot is protected");
        System.out.println("PASS: fragment hosts, response-popup stacks/subclasses and content root protected; ad slots unchanged");
    }
}
