# Standalone VPN Gate image retrieval — implementation status

## Required behavior

- Patched ChMate only: no dependency on another installed VPN application.
- Experimental and default off; clearly disclose VPN Gate relay/logging risks.
- Protect native image/thumbnail requests to the exact `tools.yajuvideo.st` host.
- Preserve a protected per-download route across every redirect and retry.
- Resolve protected destinations inside the tunnel; no default-network DNS.
- Disconnect/timeout/invalid certificate/unsupported protocol means no image
  retrieval, not direct fallback. Never label connection initiation as protection.
- Ordinary board/posting/image traffic must remain on its existing route.
- Do not contact reported tracking URLs during development or validation.

## Selected architecture (not enabled yet)

An in-process image-only tunnel, rather than an OS default-route VPN, avoids
changing native ChMate traffic or disconnecting an existing device VPN. It needs
a private TCP/UDP stack over negotiated VPN packets, tunnel DNS, and an image
downloader using that stack. Native attachment URLs can be mapped to a
process-owned loopback relay only after all native download paths are verified.
That relay must not be an unauthenticated, general-purpose forwarding proxy.
Downloads require bounded size/time/concurrency, public-address validation for
all resolved destinations, redirect validation and normal image TLS validation.

VPN Gate exposes VPN servers, not HTTP/SOCKS proxies. An embedded protocol engine
is necessary. An MIT-licensed, Java/Kotlin candidate has been inspected:
https://github.com/kittoku/Open-SSTP-Client
Its existing IPTerminal uses Android VpnService and is **not** suitable unchanged:
it needs an image-only private packet transport and a separate TCP/UDP stack.
The selected protocol core has been vendored at revision
`5f97511c3afff7dcf76b763882e0e29207ad1a36`, retaining its MIT notice. OS VPN/UI
code is excluded. `SSLTerminal` is replaced with strict JSSE chain/hostname
validation using the official catalogue profile's trust anchor; `IPTerminal`
is replaced with process-private packet queues and a Go/gVisor userspace stack.
No VPN Gate server certificate checks may be silently disabled to make it work.

## Current milestone (not an enabled feature)

`ImageVpnRequestPolicy` and its offline regression test define per-download
protection: exact-host matching, redirect inheritance, redirect limits, no
credentials/non-web targets, and errors on absent/failed tunnel backends.
The optional patch now wires native HttpUrl build results and java.net initial
requests to capability-addressed loopback URLs. Attachment projection covers the
four supported native response models and keeps the displayed post text intact.
There is a runtime opt-in switch, shown only when the optional APK asset marker
is present. The switch is also default off. Full redirect/leak validation and
actual target-image display verification remain incomplete. In particular,
java.net automatic redirects from ordinary hosts into the protected host need
an explicit audit; initial-request interception alone does not prove that case.
It must not be published as fully verified IP protection.

Implemented staging components:

- `VpnGateCatalog`: bounded, TLS-authenticated official CSV bootstrap. Ignores
  scripts/directives, validates numeric public IPv4 and a CA certificate,
  rejects duplicate remotes/CAs, deduplicates candidate IPs. An OpenVPN profile
  port is not an SSTP listener advertisement: try SSTP's documented default 443
  and skip unsupported/certificate-invalid servers.
- `SstpImageSession`: only reports readiness after PPP/IP negotiation and packet
  workers start. Interruption/timeout closes sockets and stack. No OS VpnService.
- `NativeImagePacketStack`: tunnel-local DNS UDP and HTTP TCP via gVisor/gonet,
  normal image TLS validation with Android trust roots. Public IPv4 only;
  redirects stay on this transport; no ordinary-network dialer or direct fallback.
- `ImageVpnFetchManager`: process-singleton owner, serial bounded downloads, at most three connection
  candidates, worker-thread requirement, close/cancel and sanitized error states.
- `ImageVpnRelay`: loopback IPv4 only, random 256-bit capability paths for 128
  registered URLs with ten-minute expiry, bounded request headers/queue/deadlines,
  exact loopback Host validation, no arbitrary target query, no Location headers.
  Original URLs are not exposed in local addresses or error bodies.
- AAR for all four ABIs (13.5MB compressed); every ELF LOAD segment verified
  with 16KB alignment. MIT/Apache/BSD notices are bundled in MPP resources.
- `imageVpnResourcesPatch` is a separately selectable, default-off patch named
  `画像取得専用VPN（実験用・検証中）`. Selecting it includes verified native libraries
  and license assets in the APK. Ordinary Haiagaru does not depend on this patch.
  It depends on the optional native-transport hook and includes an install marker;
  its description discloses the unverified image-path/leak tests and relay risks.
  Its compatibility follows Haiagaru; the eventual loader integration targets
  191/226/241/242 (243 remains deprecated).

The fetch policy is based on the exact host, not a filename whitelist. It accepts
valid HTTP(S) image URLs such as `/om/todoroki-hajime.jpg` without requiring a
special case. This is URL validation only: a successful actual image fetch or
attachment display has not yet been verified for that URL. The user clarified
that this image's expected contents are a white 1x1 image, so its appearance alone
does not demonstrate a rendering failure or prove the transport route.

## Emulator diagnostic evidence

The isolated debug APK negotiated SSTP/PPP against the official catalogue
candidate `public-vpn-226.opengw.net` at `219.100.37.201:443`. The private-stack
fixed `https://api.ipify.org/` diagnostic returned exit IP `219.100.37.245`.
This verifies actual DNS/TCP/TLS communication through the userspace tunnel, not
only connection initiation. Probe timestamps are from the emulator's clock:
PPP_READY 10-14 17:22:13.882; EXIT_ADDRESS 17:22:15.592.

The command to start a target-image relay probe was rejected by the execution
environment and was not retried via an alternate route. The currently visible
ChMate package `jp.co.airfront.android.a2chMate.haiagaru.ngmanage191` was then
updated in place with the optional VPN patch using `adb install -r`, preserving
application data. The user enabled the runtime switch. Read-only Frida inspection
confirmed `available=true`, `enabled=true`, `IMAGE_VPN_CONNECTED` and a connected
SSTP session. At the next inspection the relay worker was waiting and the settings
dialog was foregrounded. Neither this connected state nor a white 1x1 thumbnail
alone establishes completed image delivery or absence of direct-network leaks.
After the user returned to the thread, a screenshot showed the expected white
`1x1` native thumbnail, with no remaining download-progress indicator. This
confirms visible image display; full routing/leak assertions still require the
network and redirect checks listed below.

Passed checks: Android extension compilation and patch-bundle build; offline Go
private-DNS/disconnect/address tests; JVM routing-policy, catalogue-parser and
authenticated-loopback-relay fixtures. These tests use fake target paths/backends
and do not contact the reported tracking host. They are not end-to-end leak tests.

Rebuild native code with `scripts/BuildImageVpnStack.ps1` (local Go 1.25.1 and
Android SDK/NDK paths required). Recheck native checksums/alignment after rebuild.

## Remaining work

2026-10-15 update: java.net automatic redirect bypass was reproduced on the JVM
and inside the installed Android 191 app using only loopback test servers.
See `image-vpn-validation-20261015.md`. The identified bypass was subsequently
fixed and its JVM/Android loopback regressions pass; remaining all-path release
verification must not be confused with the completed regression fix.

1. Verify the real image-relay/download/display pipeline with installed patched
   native apps, rather than treating the fixed exit-address probe as image proof.
2. Audit manual loads, redirects entering the protected host, java.net and OkHttp,
   and the force-HTTPS option's loopback exemption. A mapping failure must not
   return the original protected URL or crash the UI. Validate native parser
   contracts against 191/226/241/242; 243 remains deprecated.
3. Exercise disable/re-enable, stale capability URLs, disconnects and retries
   under network capture; test normal board/posting traffic remains unchanged.
4. Opt-in settings include connection/failure states and explicit warnings:
   VPN relay operators see the source IP and may log traffic; unique image URLs
   can still identify viewers. This is not complete anonymity.
5. Capture IPv4/IPv6/DNS traffic under disconnect, redirects and retries; prove
   that no protected destination receives a direct connection. Until then the
   feature remains default-off and explicitly marked experimental/unverified.
   The 1.8.0 prerelease has not been published; these checks remain release gates.
