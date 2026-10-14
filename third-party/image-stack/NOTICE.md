# Image VPN userspace stack dependencies

The prebuilt Android AAR is built from `vpn/image-stack/stack.go` and the pinned
`go.mod`/`go.sum`. It contains Go's runtime and gomobile Java/JNI bindings.

- Go 1.25.1 — https://go.dev/ — BSD-style license (`go/LICENSE`).
- gVisor `1c24d89e5808` (Go branch) — https://github.com/google/gvisor — Apache-2.0 (`gvisor/LICENSE`).
- google/btree v1.1.2 — https://github.com/google/btree — Apache-2.0 (`btree/LICENSE`).
- golang.org/x/mobile `6912353760cf` — BSD-style (`x-mobile/LICENSE`).
- golang.org/x/sys v0.36.0 — BSD-style (`x-sys/LICENSE`).
- golang.org/x/time v0.7.0 — BSD-style (`x-time/LICENSE`).

The SSTP protocol engine has its separate MIT notice in `../open-sstp`.
This inclusion does not imply endorsement by these projects or VPN Gate.
The protocol engine and gVisor stack are adapted for private process-local image
traffic; no Android VpnService, TUN device, or OS default route is used.
