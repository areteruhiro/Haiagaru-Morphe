# Native NG search and Wacchoi consolidation

Supported: ChMate 191 dev / 226 dev / 241 / 242 dev.

Haiagaru settings → 登録NGの検索・板別表示・ﾜｯﾁｮｲ統合:

- Search registration text (case-insensitive partial matching).
- Choose NGName, NGID, word, thread, BE or image registrations.
- Filter all boards, global rules only, or a specific registered board key.
- NGName: preview complete literal Wacchoi names with shared last four chars.
  Only groups with identical board and non-matching-mode native flags are eligible.
  Legacy literal mode 0 and explicit literal mode 0x10 are both supported.
  Regex and numeric/special matching modes are excluded from source deletion.
- Select groups explicitly; optional deletion defaults OFF. Native regex rules
  preserve board/flags and match carrier variants while keeping suffix case.
- The entire original snapshot is rechecked immediately before applying.
  A changed list or exceeded registration cap cancels the operation.

Long press → ﾜｯﾁｮｲ下4桁をコピーしてNG編集 now copies a literal fragment such
as `-Kuom|` and opens the stock NGName editor without writing any new row.
The earlier extended direct-registration popup is superseded; board, chain,
and hide choices are made while editing a rule in the native editor.

This implementation does not replace the stock NG settings list; the enhanced
search/filter UI is reached from Haiagaru settings. Stored suffix rules remain
regular expressions in the stock list. Reopen displayed threads after batch
consolidation to ensure their native NG cache is reevaluated.

Raw snapshot methods verified against existing original-APK smali:
191: a(), 226/241: d(), 242: e(). These are copies of all registered items;
the other public ArrayList getter can exclude expired entries. Avoid selecting
an arbitrary method through reflection enumeration order.

Tests: VerifyNgNameConsolidation (scope/flags/case/symbols/exclusion),
VerifyWacchoiSuffixNg (registration flags and regex). No release requested.

2026-10-15 emulator validation: installed isolated `HaiagaruNG191` package.
Frida only seeded synthetic native items and invoked the real dialogs; no
method behavior was replaced. The manager loaded all six NG categories.
Two synthetic names in one board (flags 9: chain + hidden) were consolidated
with deletion selected; the native store then contained the suffix regex in
that board with flags 41 and the unchanged name in a different board (flags 0).
Screenshots: build/verification/ng-management.png and ng-after.png.
Actual 226/241/242 UI and device behavior still require validation.
The extended registration popup displayed all-board/current-board/existing-board
choices. Selecting the synthetic current board and registering with both flags
unchecked updated only its existing regex (flags 32); the other board remained
unchanged. This checks scope-aware update without adding duplicate suffix rules.

## 1.8.0 report: ordinary literal rules excluded

The report shows `(ﾜｯﾁｮｲ 8776-Kuom)` and `(ﾜｯﾁｮｲ 87f7-Kuom)` on the
same android board, both with chain and hidden enabled. The candidate filter
incorrectly excluded every nonzero bit in 0x70, including ordinary literal
mode 0x10. Original 191 NGWord.Item.e()/b() confirm 0x10 means quoted literal,
whereas 0x20 means regex. A fixture with flags 0x19 reproduced the rejection.

The fix accepts both literal modes and groups them by the resulting regex
flags, preserving board, chain, hidden, disabled and other non-mode flags.
Writing the rule now replaces 0x70 with 0x20 rather than OR-ing 0x20 (which
would leave invalid mode 0x30). Preview selection, optional deletion (off by
default), registration limit and stale-snapshot protections remain unchanged.
The screenshot cannot establish the user's exact stored bits; the identified
literal-mode failure is reproduced using the same visible rule shape/options.

Fix verification: the red 0x19 fixture now passes, along with mixed legacy/
explicit literal grouping, exclusive regex mode and option preservation tests.
Android MPP build and patch application to 191 passed. In isolated API36
emulator package `jp.co.airfront.android.a2chMate.haiagaru.ngliteral191`, seeded
the two reported name shapes with flags 0x19 and board android. Through the
actual UI, selected the Kuom candidate and optional source deletion, then
applied. Native store contained exactly one rule with flags 0x29, board android;
chain/hidden were preserved. Other carrier names matched, lowercase kuom did
not. No production method was replaced; Frida only seeded and inspected test
data. Screenshots: `build/verification/ng-literal-preview.png` and
`build/verification/ng-literal-after.png`. User's Samsung/Android16 environment
and 226/241/242 UI are not yet checked; the corrected implementation is shared.
The published 1.8.0 asset has not been replaced and no release/commit was made.
