package app.morphe.extension.chmate;

/** Pure policy shared by the transport and regression tests. No cloud I/O. */
final class MegaBackupDeletionPolicy {
    static final String OVERWRITE = "overwriteDeviceBackup";
    static final String PERMANENT = "permanentlyDeleteOldDeviceBackup";

    private MegaBackupDeletionPolicy() { }

    static boolean permanent(boolean overwrite, boolean optedIn) {
        return overwrite && optedIn;
    }

    static boolean canRemove(boolean overwrite, boolean verified, String expectedName,
                             String oldName, String uploadedHash, String oldHash) {
        return overwrite && verified && expectedName != null
                && expectedName.startsWith("Haiagaru-sync-") && expectedName.endsWith(".zip")
                && expectedName.equals(oldName) && uploadedHash != null && !uploadedHash.isEmpty()
                && oldHash != null && !oldHash.isEmpty() && !uploadedHash.equals(oldHash);
    }
}
