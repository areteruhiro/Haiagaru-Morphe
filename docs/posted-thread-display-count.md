# Posted thread display count (191 / 226 / 241 / 242 dev)

Haiagaru settings offers “書き込んだスレの表示数（0で無制限）”, accepting
1–10000 or 0 for unlimited. Defaults without a saved preference are unlimited
on 191/226/241 and 10 on 242 (and deprecated 243). Existing saved values are
preserved. Reopen the home list after saving.

This is separate from `postDataListCount`, which controls stored post-history
retention. This feature changes only the database query limit; it does not
delete stored posts or bookmarks, change their order, or increase the number
of already deleted records. In particular, the adjacent “送信済み” section is
assembled separately from postDataList and is not limited by this setting.

Original APK inspection identified:

- 191: `o.r8lambdaTb_p0z6z2AqSZIga1YhmAVmiTPk.a(Context)` writes 10 to
  `r8lambdaEBvvDaQDWIaS7WUoordU_4sxR3Y$IconCompatParcelizer.d` after `posted !=0`.
- 226: `HomeFragment$RemoteActionCompatParcelizer.e(Context)` writes 10 to
  `TrustRootIndex$write.a` after the same posted predicate.
- 241: `o.rS.b(Context, o.KP)` queries `posted != 0`, sorted by posted time,
  with `tul11$read.e = 10`.
- 242: `o.queryPackageSignatureVerified.d(Context, o.PackageSignatureVerifier)`
  constructs `initRenderFinish$addObserverForBackInvokerlambda0` with limit 10L.
- 243: `o.zzaA.e()` constructs `zaaw$addObserverForBackInvokerlambda0` with 10L.
  The SQL in both modern query classes is `WHERE posted > 0 ORDER BY posted DESC LIMIT ?`.

Patch anchors require exactly one matching limit next to the database query
field/constructor. They do not replace unrelated Kotlin collection capacity
defaults of 10. The original APKs inspected here have query literals of 10 even
on older versions; this does not imply that the independently assembled sent
section shares that limit. The patch changes only the posted-thread query.
Zero is translated into Integer.MAX_VALUE for all query builders, not LIMIT 0.

Tests: `scripts/VerifyPostedThreadCount.java` for bounds and invalid input;
`scripts/VerifyPostedThreadCountHooks.java` for generated APK hook placement.

Validation (2026-10-15): the Android MPP build and value regression pass,
including zero/unlimited, arbitrary valid counts, invalid inputs and defaults.
All four originals (191/226/241/242) accepted the updated patch and generated
APKs. DEX inspection verified exactly one display-limit hook at the intended
query in each version. APKs are named `build/verification/chmate*-posted-unlimited.apk`.
These APKs have not been installed for UI verification; no new release was made.
