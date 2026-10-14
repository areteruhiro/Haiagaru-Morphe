# Wacchoi suffix NG registration options

## Current behaviour: copy and open native NGName editor

The long-press action is now “ﾜｯﾁｮｲ下4桁をコピーしてNG編集”. It copies the
literal suffix with a leading hyphen and trailing pipe, e.g. `-Kuom|`, then opens
ChMate's native NGName tab. It does not register a new row or modify an existing
row. Users can select an existing registration, paste the fragment and choose
board, chain and hidden options in the native editor. Existing registrations
are neither deleted nor automatically consolidated.

Raw case and symbols are preserved in the clipboard; no regex is inserted.
In regular-expression mode a trailing pipe creates an empty alternative: add
the next condition or remove the trailing pipe before saving. A toast explains
this. Clipboard/editor failures show a manual-opening fallback, never automatic
NG registration.

NativeNgEditor derives the NAME ordinal from the native editor's actual factory
enum rather than assuming a fixed tab index. The `ngType` integer intent and
factory signatures were inspected in 191/226/241/242 (and deprecated 243).
Existing popup/bottom-sheet entry points all call the same suffix action.

Offline suffix tests cover `-Kuom|`, carrier names, case and symbolic suffixes.
VerifyNativeNgEditor verifies different enum orders, nested companion factories,
safe failure for missing/ambiguous factories, clipboard invocation and absence
of registration calls from the action. Android MPP build passed.

### Copy/editor verification (2026-10-15)

- Android patch application completed successfully for 191/226/241/242.
- Installed the updated 191 APK over the isolated emulator test package,
  preserving existing data.
- Frida invoked the production action with `(ｽｯﾌﾟ Sd03-Kuom)`, without
  replacing runtime methods. Clipboard was exactly `-Kuom|`; NG entry count
  remained unchanged. The previous clipboard was restored after the check.
- The native NGEditorActivity received `ngType=2` in this 191 APK and had
  focus. Screenshot confirmed the Name tab selected:
  `build/verification/ng-suffix-copy191.png`.
- Runtime UI verification for 226/241/242 is not yet performed; their native
  editor intent contracts and menu callbacks were inspected/tested and APK
  patching succeeded. No commit or release performed for this change.
- Diagnostic helper: `scripts/frida-ng-suffix-copy.js`.

## Historical direct-registration implementation (superseded)

The existing “ﾜｯﾁｮｲ下4桁でNGName登録” confirmation dialog now includes two
independent checkboxes:

- 連鎖（対象レスへの返信もNG）
- 非表示（あぼーん扱いにしない）

Both start unchecked each time, preserving ordinary NG as the default.
Cancel does not change the store. Registration uses ChMate's own NGWord
flags, not an additional rendering-only filter: regex `0x20`, chain `0x08`,
transparent/hidden NG `0x01`. The native 191 NG editor's bulk chain and
hidden actions confirm the latter two masks.

If the same globally scoped regex exists, replace it via the native update
API instead of ignoring the new options or adding a duplicate. Preserve
its creation timestamp and unrelated options; enforce regex matching mode.
Board-scoped rules are not overwritten. Replacement is allowed even at
the registration limit. 191's explicit refresh event includes both the old
and new item so the displayed list is notified of the update.

The shared implementation is used by the existing 191 dev / 226 dev / 241 /
242 dev long-press menu hooks. No new patch toggle or settings entry is needed.

## Verification (2026-10-15)

- Patch bundle build passed.
- `VerifyWacchoiSuffixNg.java`: extraction, escaping, case-sensitive suffixes,
  all four checkbox combinations, unrelated-bit preservation, and exclusive
  regex mode passed.
- Existing `VerifyWacchoiNgPopup.java` and `VerifyWacchoiNameSheet.java` passed.
- 191 test APK installed on API 36 emulator. Opened the actual confirmation
  method using Frida with a synthetic suffix only (no runtime implementation
  hooks), then selected both options and registered through the UI.
  Native NGName entry flags were `41` (`0x29`), count one.
- Reopened the same rule, left both unchecked, and registered. Flags became
  `32` (`0x20`), still one entry: updates can turn both options off without
  creating duplicates.
- Checkbox labels and layout visually inspected in screenshot
  `build/verification/frida-emulator/ng-options-checked.png`.
- Physical device and per-version end-to-end checks for 226/241/242 have not
  been performed for this addition. No release or commit performed.

Diagnostic helper: `scripts/frida-ng-suffix-options.js` (test app only).
