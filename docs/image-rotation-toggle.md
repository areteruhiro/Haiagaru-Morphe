# Image viewer rotation button switch

Haiagaru settings now exposes `画像ビューアの回転ボタン` for ChMate
0.8.10.191 dev and 0.8.10.226 dev. Default on preserves existing behavior.
The stored key is `imageViewerRotationButton` in Haiagaru's UI-config preferences.

The overlay observer hides the button with GONE while disabled and does not
reposition/search counters on that path. The click listener also checks the
setting. Turning it on restores the prior native-counter visibility behavior
without replacing ChMate's image gesture listeners. Disabling the control does
not reset an image's current orientation.

The user wrote 228; the existing patch targets 226 dev, not 228. No 228 APK or
rotation hook has been confirmed, so this change does not claim 228 compatibility.
