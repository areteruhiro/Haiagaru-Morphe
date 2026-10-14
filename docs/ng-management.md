# Native NG search and Wacchoi consolidation

Supported: ChMate 191 dev / 226 dev / 241 / 242 dev.

Haiagaru settings → 登録NGの検索・板別表示・ﾜｯﾁｮｲ統合:

- Search registration text (case-insensitive partial matching).
- Choose NGName, NGID, word, thread, BE or image registrations.
- Filter all boards, global rules only, or a specific registered board key.
- NGName: preview complete literal Wacchoi names with shared last four chars.
  Only groups with identical board and all native flags are eligible. Regex,
  wildcard and alternative matching modes are excluded from source deletion.
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
