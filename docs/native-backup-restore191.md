# 191 native ZIP restore safety

Scope: ChMate **0.8.10.191 dev**, standard Settings → Other → Backup/restore.
This is separate from Haiagaru JSON/local/MEGA restore. No native restore hooks
are added to 226/241/242/243.

## Findings

Original 191 `o.setFormat.onStart()` deleted `cache/restoreTmp` and recopied and
extracted the entire selected ZIP on every start. `onStop()` interrupted the
restore worker and discarded its reference. The worker tested cancellation with
`Thread.interrupted()` repeatedly, clearing the flag before deciding whether to
post success. No explicit seconds-based timeout was found in these paths.

Success deferred installing `databases/roidon.sqlite` via `boot_prefs.rdp` until
the next process startup. Startup cleared that preference, ignored the DB-copy
result, and deleted `restoreTmp` even on failure. A successful log-file copy did
not establish that the index database had been installed.

These are confirmed code defects, not proof of the precise failure on the
reported Samsung Android 16 device. The user's 242 stable archive is unavailable;
the locally available source APK is 242 dev, not the same build.

## Changes

- Retain the restore fragment across configuration changes; serialize preview
  work and prevent screen stop from cancelling an in-flight restore or losing
  its worker reference.
- Reuse an already prepared extraction on resume; rebuild the preview adapter
  for the recreated view without rereading/reextracting the ZIP.
- Keep expanded working data in `no_backup/restoreTmp`, not Android's cache
  eviction area. Redirect native startup cleanup to the same directory.
  Android cache reclamation during the user's failed attempt is unconfirmed.
- Reject overlapping restore workers. Check the source DB and additional free
  space before the native worker starts replacing destination directories.
  File sizes and per-filesystem totals use `long`, with a 64 MiB reserve. This
  conservative check does not count space that native replacement might free.
- Preserve the interrupt flag during all native worker cancellation tests.
- Keep the real SQLite failure in preview errors/logcat instead of claiming
  that every SQLite exception means the selected ZIP is not a ChMate backup.
- Stage the DB in its destination directory, sync, check size/SHA-256 and SQLite
  integrity plus the columns 191 reads. Then rename the previous DB to a unique
  `roidon.sqlite.before-restore-*` recovery file before installing the stage.
  Do not force a newer schema/version number down to 191's version.
- If installation fails, retain the pending DB path and `restoreTmp`, skip the
  native cleanup, log `NATIVE_RESTORE_DB_INSTALL_FAILED`, and show the failure
  on the next resumed activity. An existing nonempty WAL/journal is rejected
  rather than ignored. The original ZIP is never edited or deleted.

This is not a whole-archive transaction: native DAT/preferences copying still
uses ChMate's implementation. Process death, device/storage failure, changing
available space during copying, or deliberately leaving the restore activity
can still require retrying from the original ZIP. The recovery DB file is
intentionally kept after success; it does not by itself snapshot every DAT file.

## Verification

`scripts/VerifyNativeRestoreFiles.java`: streaming install, injected validation
failure, cancellation without consuming its flag, nonempty WAL rejection,
source/old-DB preservation, invalid and identical paths, staging cleanup, and
64-bit size accounting using a sparse file over 3 GiB.

`scripts/VerifyNativeRestoreHooks.java`: inspect patched DEX, not source only;
assert lifecycle, extraction reuse, interruption and startup installation hooks.

`scripts/frida-native-restore191.js`: refuses any package except the isolated
`jp.co.airfront.android.a2chMate.haiagaru.restore191`. Synthetic 1,000-DAT ZIP,
preview parsing, retained fragment, background/resume sentinel, native restore
worker and next-start DB installation. Inject an invalid pending DB and verify
that the previous DB, source, pending path and error all survive restart.

Not verified with the reported 1.95 GiB / roughly 18 GiB / 200,000-file archive,
nor on the reported Samsung Android 16 device. Included in the 1.8.1 prerelease;
the large-archive migration remains unverified.
