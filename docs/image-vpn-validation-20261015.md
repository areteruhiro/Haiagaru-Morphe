# Image VPN validation — 2026-10-15

## Original result: release gate failed; identified java.net bypass now fixed

Android java.net automatic redirects bypass the app's URL routing wrapper.
This is a confirmed missing enforcement point, not only an untested possibility.
The initial diagnostic did not modify production code. The subsequent requested
fix adds ImageVpnRedirectConnection and integrates it into HttpsTransport only
while the optional image VPN is enabled.

## Reproduction without external traffic

`scripts/VerifyImageVpnJavaRedirect.java` compiles the production HttpsTransport
with a controller test double. Two loopback HTTP servers emulate an ordinary
source and a destination which the test controller would block if consulted.
The source returns HTTP 302. With automatic redirects enabled, the destination
receives one direct request, but route() is invoked only once for the source.
The privacy regression intentionally fails with
`IMAGE_VPN_JAVA_REDIRECT_BYPASSES_ROUTING`.

`scripts/frida-imagevpn-java-redirect.js` repeats the two-loopback-server test
inside the installed 191 app on emulator-5558 (PID 15614 at inspection time).
It calls the real HttpsTransport and observes the real controller URL route,
without changing the persisted settings or touching external image URLs.
Android output:

```
ANDROID loopback route callbacks=1, direct destination requests=1
CONFIRMED: java.net automatic redirect bypasses route callback
```

This establishes that an ordinary URL's redirect can skip protection before a
destination connection. It does not claim an observed external target IP leak:
both reproduced connections stayed on 127.0.0.1. No DNS/contact to the reported
image host was needed for this test.

## Passed checks

- Go: TestPrivateImageAndRedirectPipeline, TestPublicAddresses,
  TestOnlyPrivatePacketLinkForDNS, TestFailClosedAndInputs. The HTTP peer and DNS
  server use a virtual gVisor packet link; no ordinary OS network dialer.
- JVM: VerifyImageVpnController (marker, opt-in, ordinary URLs, native parser
  recursion, attachment projection, disable cleanup).
- JVM: VerifyImageVpnRelay (capabilities, bounded entries, error responses without
  redirects, backend disconnect, cleanup).
- JVM: VerifyImageVpnRequestPolicy (routing ticket state, redirect inheritance,
  disconnect and absence of direct fallback). These ticket tests do not establish
  that the ticket is consulted by platform automatic redirects.
- VerifyImageVpnPatchSelection.ps1: separate optional patch, default-off and
  visible experimental limitations.
- Earlier emulator evidence: real private-stack exit-address probe and 191
  white 1x1 image display. These are not full leak-test proof.

## Required before release

Enforce destination routing before java.net redirect DNS/connect without
breaking normal redirect behaviour, HTTP method/body handling, cookies or TLS
validation. Re-run the red regression until it passes, then exercise Android
and native OkHttp paths, force-HTTPS, disconnect and retry scenarios. Other
version/device end-to-end routing tests remain outstanding. No 1.8.0 release
or bundle metadata was published during this verification.

## Requested fix and re-verification

Platform automatic redirects are disabled on the underlying connection. The
HTTP/HTTPS facades follow each redirect themselves through openRawConnection,
which applies HTTPS policy and target routing before opening the new socket.
Failures close the connection and never retry the original destination directly.
An in-flight guarded request also rejects a protected redirect after disable.

The JVM regression now reports route callbacks=2, direct destination requests=0.
An additional synthetic relay test delivers the body through the mapped local
endpoint with force-HTTPS both off and on, without contacting the synthetic
protected endpoint directly.

VerifyImageVpnRedirectCompatibility passes ordinary GET, buffered POST
302/303 conversion and 307/308 replay, cross-origin credential stripping,
final response headers/body/URL, redirect disabling, streamed-body retry refusal
and bounded loops. VerifyImageVpnRedirectTls checks the HTTPS facade, TLS factory
and verifier preservation and same-origin credentials without real TLS sockets.
Existing controller, relay and routing-ticket regressions still pass.

The updated Android MPP built successfully. A fresh 191 APK with the optional
VPN patch was installed using adb install -r into the same package, preserving
data. The app started (PID 17036 during verification) without an observed startup
crash. The Android loopback regression, using a test-only hook to reject the
synthetic target before connection, reports:

```
ANDROID loopback route callbacks=2, direct destination requests=0
PASS: guarded redirect consulted routing and blocked synthetic target
```

The identified java.net bypass is fixed. These tests are not a blanket anonymity
claim or full external packet-capture coverage. Other versions and native
OkHttp end-to-end paths still need verification before the planned release.
