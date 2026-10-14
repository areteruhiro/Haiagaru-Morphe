# 191 / 226 image-viewer rotation control

The Haiagaru-added rotation control retains its backgroundless glyph and follows
the native image counter's visibility. Its touch target is now 56dp square,
with a 32dp bottom gap and 24dp end gap.

Navigation-bar, display-cutout and system-gesture insets keep the control clear
of Android navigation buttons. Insets already excluded by the content host's
geometry or padding are subtracted to avoid excessive spacing. Placement follows
RTL layout and landscape side navigation bars without replacing ChMate's native
window-inset listener. Layout is updated only when margins change.

241 / 242 native rotation controls are unchanged.

Verification: `:patches:build` passed; `VerifyViewerControlInsets.java` covers
three-button / gesture navigation, fitted and partially fitted hosts, side
navigation and missing insets. Device-screen confirmation is still pending.
