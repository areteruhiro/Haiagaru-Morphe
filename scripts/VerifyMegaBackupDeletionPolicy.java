package app.morphe.extension.chmate;

/** Does not contact MEGA or delete files. */
public final class VerifyMegaBackupDeletionPolicy {
    private static int checks;
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
        checks++;
    }
    public static void main(String[] args) {
        String own = "Haiagaru-sync-Android-install-A.zip";
        String other = "Haiagaru-sync-Android-install-B.zip";
        check(!MegaBackupDeletionPolicy.permanent(true, false), "Default is trash");
        check(!MegaBackupDeletionPolicy.permanent(false, true), "No permanent deletion without overwrite");
        check(!MegaBackupDeletionPolicy.permanent(false, false), "Both disabled");
        check(MegaBackupDeletionPolicy.permanent(true, true), "Explicit opt-in permits permanent deletion");
        check(MegaBackupDeletionPolicy.canRemove(true, true, own, own, "new", "old"), "Verified exact old backup eligible");
        check(!MegaBackupDeletionPolicy.canRemove(true, false, own, own, "new", "old"), "Failed verification never removes old");
        check(!MegaBackupDeletionPolicy.canRemove(false, true, own, own, "new", "old"), "New-file mode never removes old");
        check(!MegaBackupDeletionPolicy.canRemove(true, true, own, other, "new", "old"), "Another device is excluded");
        check(!MegaBackupDeletionPolicy.canRemove(true, true, own, own, "same", "same"), "New upload itself is excluded");
        check(!MegaBackupDeletionPolicy.canRemove(true, true, own, own, "", "old"), "Missing new hash rejected");
        check(!MegaBackupDeletionPolicy.canRemove(true, true, own, own, "new", null), "Missing old hash rejected");
        check(!MegaBackupDeletionPolicy.canRemove(true, true, "unrelated.zip", "unrelated.zip", "new", "old"), "Non-backup excluded");
        check(!MegaBackupDeletionPolicy.canRemove(true, true, null, own, "new", "old"), "Missing filename rejected");
        System.out.println("PASS " + checks + " MEGA backup deletion policy checks (no cloud writes)");
    }
}
