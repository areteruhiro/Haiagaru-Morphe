# 191 dev: tablet toolbar settings dialog opens twice

## Cause

In tablet mode, the thread bottom toolbar's menu uses `o.m6b$4.Xf_(MenuItem)`.
The callback calls the originating fragment's `VQ_`, but even if it returns
`true` (handled), it falls through to `Activity.onMenuItemSelected`.
The activity dispatches the same item to the board pane as well.

Frida tracing on API 36 showed two `o.p8a.e("listToolbarSetting", ...)`
factory calls for one toolbar settings tap:

1. `o.pa` (thread pane), via the popup callback's fragment dispatch.
2. `o.r8lambdaTb_p0z6z2AqSZIga1YhmAVmiTPk` (board pane), via the activity.

Thus pressing OK dismisses one of two already-open dialogs; it does not
create a new dialog after saving.

## Fix

`patchConsumedPopupMenu191()` returns the fragment's nonzero handled result
before the native activity fallback. Unhandled items keep their original
activity dispatch. This applies only to 0.8.10.191 dev and does not change
dialog save listeners, title menus, preferences, or introduce debounce timers.

## Verification (2026-10-14)

- Built `:patches:build` successfully.
- Patched original 191 APK with test package
  `jp.co.airfront.android.a2chMate.haiagaru.toolbar191`.
- Installed on root API 36 emulator `emulator-5558`, tablet mode enabled.
- With Frida detached and app restarted: bottom menu → toolbar settings → OK
  returns to the thread after one tap; no second settings dialog remains.
- Reopen → Cancel also returns normally; no AndroidRuntime exception observed.
- `scripts/VerifyToolbar191Menu.java` simulates boolean control flow directly
  from the patched APK for all four fragment/activity handled combinations.
  Fixed APK passes; baseline fails with an unwanted activity dispatch when
  the fragment already handled the event.
- Physical tablet verification and release publication have not been performed.

Artifacts:

- `build/verification/image-rotation/chmate191-toolbar-fixed.apk`
- `build/verification/frida-emulator/toolbar-fixed-ok.png`

Regression check (JDK 21):

```powershell
javac -cp C:/Users/narum/Downloads/morphe-desktop-1.13.0-all.jar -d build/verification scripts/VerifyToolbar191Menu.java
java -cp 'build/verification;C:/Users/narum/Downloads/morphe-desktop-1.13.0-all.jar' VerifyToolbar191Menu build/verification/image-rotation/chmate191-toolbar-fixed.apk
```
