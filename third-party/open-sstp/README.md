# Embedded SSTP protocol core

Source: https://github.com/kittoku/Open-SSTP-Client
Revision: 5f97511c3afff7dcf76b763882e0e29207ad1a36
License: MIT (KOBAYASHI Ittoku).

Only protocol/cipher/frame helpers are reused. Namespace is relocated to
app.morphe.extension.chmate.vpn.sstp. The upstream OS VPN service, TUN terminal,
UI, network observer and SSL terminal are excluded. Host/SSL/IP adapters are
implemented for a private image-only packet stack; no OS route is installed.
Controller removes the OS VPN observer. Native networking integration remains
experimental and is not enabled in production until leak tests pass.
