// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.unit.ppp

import app.morphe.extension.chmate.vpn.sstp.unit.ppp.option.Ipv6cpOptionPack
import java.nio.ByteBuffer


internal abstract class Ipv6cpFrame : Frame() {
    override val protocol = PPP_PROTOCOL_IPv6CP
}

internal abstract class Ipv6cpConfigureFrame : Ipv6cpFrame() {
    override val length: Int
        get() = headerSize + options.length

    internal var options: Ipv6cpOptionPack = Ipv6cpOptionPack()

    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)

        options = Ipv6cpOptionPack(givenLength - length).also {
            it.read(buffer)
        }
    }

    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)

        options.write(buffer)
    }
}

internal class Ipv6cpConfigureRequest : Ipv6cpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_REQUEST
}

internal class Ipv6cpConfigureAck : Ipv6cpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_ACK
}

internal class Ipv6cpConfigureNak : Ipv6cpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_NAK
}

internal class Ipv6cpConfigureReject : Ipv6cpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_REJECT
}

