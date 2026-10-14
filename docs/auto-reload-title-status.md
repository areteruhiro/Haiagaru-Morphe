# Auto-reload status in 241 / 242 dev

The thread display title gets a prefix such as `【自動10s】` while the
Haiagaru auto-reload session is active. Prefix placement keeps it visible
even when a long title is truncated. Only the native title-bar display setter
is intercepted, never the thread model or copied/saved title.

The displayed seconds are the ceiling of the effective interval, including
board minimum and no-new-post backoff. The indicator appears during scrolling,
waiting and loading, and is removed on manual stop, tab change, pause,
destroy-view, terminal reload event or session failure.

Weak-key per-title-bar state retains the most recently supplied native title.
Internal refreshes do not recapture a decorated title, so repeated reloads
cannot accumulate prefixes. In tablet mode the fragment's own title-bar holder
is used (241 RoidonListFragment.f / 242 .a), not an activity-wide title search.

Tests: VerifyAutoReloadTitle, VerifyLegacyAutoReloadTiming and
VerifyLegacyAutoReloadHooks (generated APK title setter and lifecycle hooks).
VerifyAutoReloadTitleState extracts the actual extension logic to verify start,
stop, backoff, live native-title changes, no redundant redraw, hidden titles,
and per-title-bar isolation without replacing production behavior.

Validation 2026-10-15: bundle build, format/timing/state tests and patch
application to both original 241/242 APKs passed. Both generated APKs have the
title-display hook and unchanged timer lifecycle hooks. Actual device/emulator
screen confirmation has not yet been performed for this indicator.
