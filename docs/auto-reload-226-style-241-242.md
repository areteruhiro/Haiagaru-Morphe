# 241 / 242 dev: 226-style auto reload

## User behavior

- Long-press the auto-scroll toolbar button to start; long-press again to stop.
- Start scrolling at the current position, using ChMate's current auto-scroll speed.
- Preserve response ordering and filters; do not jump to the last response.
- At the bottom, wait the configured minimum, reload through ChMate's own client,
  scroll through new responses, then wait again.
- Configure **Haiagaru settings → 自動リロードの最小間隔（秒、10〜1800）**.
  Default: 10 seconds. Explicit `CLIENT_MIN_RELOAD_SEC` board rules still take
  precedence, including boards where periodic reload is disabled.
- Repeated no-change/error results increase the wait (130%, capped at 30 minutes).
  A success resets it to the minimum. This is a minimum, not a fixed polling interval.
- Stop on leaving the tab, pausing/destroying its view, or losing foreground focus.
  No background service or separate network client is introduced.

The original 226 ringtone/TTS preferences are not ported in this change.
191 / 226 behavior and deprecated 243 are not changed.

## Implementation

The original 241/242 long-press validates the board and closed-thread status.
It then enters `ResListTailingController` state. Collectors watching this state
force response ordering and immediately move to the end.

`patchLegacyAutoReload` keeps the validation but redirects its two start sites
to `LegacyAutoReload`. The tailing state remains idle, so both collectors are
bypassed without altering normal filtering/sorting code. Default timing literals
are replaced with the user preference; the explicit board-limit branch uses a
bounded maximum of the board limit and user preference.

The adapter uses verified mappings, resolved once at startup:

| Native operation | 241 | 242 dev |
| --- | --- | --- |
| Response RecyclerView | fragment `m` | fragment `n` |
| View-model lazy | fragment `s` | fragment `t` |
| Start normal auto-scroll | base `q()` | base `r()` |
| Stop normal auto-scroll | base `p()` | base `p()` |
| Reload (stock refresh button; returns whether accepted) | model `b(true)` | model `b(true)` |
| Native loading StateFlow | model `al`, getter `b()` | model `ai`, getter `d()` |
| Native configured minimum (Kotlin Duration) | controller `g` | controller `j` |
| Auto-scroll state callback | fragment `e(boolean)` | fragment `b(boolean)` |
| Active-tab callback | fragment `a(boolean)` | fragment `e(boolean)` |
| Reload-result event handler | static `a(fragment, honorsDebugCertificates)` | static `e(fragment, zzacz)` |

Native scroll animation handles scrolling. A foreground-only 250 ms main-thread
check waits for bottom arrival and reload completion. It neither allocates images
nor queries the response database. Sessions retain only weak fragment/view-model/
view references and remove pending callbacks when stopped.

The patch fails fast if version-specific anchors, field types, method signatures,
or reload event classes no longer match. Other toolbar long-press actions are
left untouched.

## Verification

`VerifyLegacyAutoReloadTiming` exercises bottom-only waits, reading new responses,
no duplicate in-flight requests, adaptive backoff, limits, and both Kotlin Duration
encodings. Compile/build and patch application results are tracked separately from
physical-device UI verification; do not infer device verification from a build.

## Physical-device follow-up

Mi Note 10 / 242 dev initially confirmed start/stop and the first reload request
approximately 10 seconds after start at the bottom. The first version could then
remain in its own loading state: it used model `i()` (navigation emission) and
assumed an accepted network reload and a delivered completion event.

The follow-up calls the native client `b(true)` directly, handles its Boolean
acceptance result, and checks ChMate's own loading StateFlow before every request.
If completion events are absent, the native idle state releases the wait, with
adapter count comparison for new responses. It never bypasses an active client
or cancels a request just to meet the configured interval.

The v2 APK was installed over the existing 242 package on Mi Note 10, keeping
app data. Passive Frida traces (no replacement of request/result logic) confirmed:

- `b(true)` accepted the network requests three times in succession.
- Reload start and `zzacz$read` (no-change) result events arrived each time.
- The scheduler returned to `loading=false`, rather than waiting forever.
- At the configured 10-second minimum, successive request timestamps were
  `1791554913517`, `1791554924708`, `1791554938577` (milliseconds). The third
  interval included the expected 130% backoff after repeated no-change results.
- The test session was stopped via the same toolbar long-press handler.

241 v2 passed patch application and signed-APK hook verification, but was not
installed on this device. A Frida `deoptimizeEverything()` attempt on the initial
APK aborted ART; the app was restarted and that operation was not repeated in v2
verification. Do not classify that diagnostic-agent crash as a patch-only crash.
