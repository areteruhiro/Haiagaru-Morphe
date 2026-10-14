# Kenmo (poverty) checker routing

For 191 dev / 226 dev / 241 / 242 dev, the shared checker Activity now prefers
Kyodemo for the `poverty` board when the checker mode is automatic. Explicit
hissi.org or Kyodemo selection remains unchanged. “Both” retains a previously
saved choice, with Kyodemo as its initial default for this board.

Live Kyodemo inspection (2026-10-15) confirmed:

- Board: https://www.kyodemo.net/sdemo/b/poverty/
- Reported thread: https://www.kyodemo.net/sdemo/r/poverty/1791989395/
- Source: https://greta.5ch.io/test/read.cgi/poverty/1791989395/

Regression tests verify greta/itest 5ch.io template host acceptance, poverty
board slug, date/ID transport (including + and /), source-thread return URL,
and unchanged automatic preferences for unrelated boards.

## Menu visibility investigation

The user reports that the ID long-press menu is missing on all four versions
despite enabling the native setting. This remains **unresolved** for their
installed APKs; do not describe the routing change as fixing menu visibility.

On the current locally patched 191 test app, with native preference
`bbs.2ch_net.menu1.enable` enabled, ID long-press on the reported thread
displays “必死チェッカーもどき”. Its expanded URL contains board `poverty`,
source host `greta.5ch.io`, correct thread key/date, and selected ID.
No poverty exclusion occurs in the stock checker templates extracted from
the four original APKs. Tests of the shared template rewrite also pass.

The user's installed Haiagaru version and a screenshot were requested to
compare applied contents. No physical-device reproduction or release done.

Diagnostics: `scripts/InspectHissiTemplates.java`, `scripts/frida-poverty-hissi.js`.

Follow-up emulator check (2026-10-15): existing `HaiagaruToolbar191` app again
shows the native menu on ID long press in the reported poverty thread. Selecting
it opens the dedicated viewer, but this older installed APK selects hissi.org
and the site returns “ファイルが存在しません”. This is a different result
from missing-menu reproduction, and does not validate the new Kyodemo-preferred
routing in the rebuilt bundle. No claim of resolving the user's missing menu.
