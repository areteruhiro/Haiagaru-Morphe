import app.morphe.extension.chmate.UpdateProgressGate;

public final class VerifyUpdateProgressGate {
    static int checks;
    static void check(boolean value) { checks++; if (!value) throw new AssertionError("check " + checks); }
    public static void main(String[] args) {
        UpdateProgressGate gate = new UpdateProgressGate();
        Object first = new Object(), second = new Object();
        check(gate.allow(first, 1, 1000, 0));
        check(!gate.allow(first, 2, 1000, 1));
        check(!gate.allow(first, 500, 1000, 99));
        check(gate.allow(first, 700, 1000, 100));
        check(gate.allow(second, 1, 1000, 101));
        check(gate.allow(first, 1000, 1000, 101));
        check(gate.allow(first, 1, 1000, 102));
        check(gate.allow(first, 2, 2000, 103));
        check(gate.allow(first, 1, 2000, 104));
        check(gate.allow(first, 2, 2000, 1));
        check(gate.allow(first, 0, 2000, 2));
        check(gate.allow(first, 5, 0, 3));
        check(gate.allow(first, 5, -1, 4));
        check(gate.allow(first, 2001, 2000, 5));
        int sent = 0;
        for (int i = 1; i <= 10000; i++) if (gate.allow(first, i, 10000, i / 10)) sent++;
        check(sent == 11); // first, each 100 ms before completion, and forced final completion
        System.out.println(checks + " progress checks passed; 10000 events -> " + sent + " notifications");
    }
}
