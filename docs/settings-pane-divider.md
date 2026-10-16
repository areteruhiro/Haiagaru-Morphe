# Legacy settings right-edge divider

ChMate 0.8.10.191 dev and 0.8.10.226 dev install a theme-colored
`SlidingPaneLayout.setShadowDrawableLeft` in their two-pane settings fragment
(`o.pq` / `o.onDenied`). When the detail pane is off screen at `left == width`,
AndroidX still draws that drawable immediately inside the right edge. This is
particularly visible with the night and ingrs themes.

The version-scoped patch wraps only this settings drawable. Its original width,
height, color and rendering are preserved for an interior pane separator; no
separator is painted against either outside edge. It does not disable scrolling,
change preferences, recolor the settings screen, or change response dividers.
241/242 are not patched by this fix.

Verification: `scripts/frida-settings-edge-test.js` uses isolated emulator apps.
On the pre-fix 191 APK, drawing the real settings pane with/without its shadow
produces different right-edge pixels, reproducing the artifact. On patched APKs
it checks hook installation, equality to the shadow-free right edge, both outer
edges, an interior/tablet boundary, and null handling. These geometry tests do
not depend on a particular theme color; they are not physical-device theme QA.

Build/patch/runtime logs: `build/verification/settings-edge*` (local artifacts).
