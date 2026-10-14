// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.unit.ppp

import app.morphe.extension.chmate.vpn.sstp.unit.ppp.option.IpcpOptionPack
import java.nio.ByteBuffer


internal abstract class IpcpFrame : Frame() {
    override val protocol = PPP_PROTOCOL_IPCP
}

internal abstract class IpcpConfigureFrame : IpcpFrame() {
    override val length: Int
        get() = headerSize + options.length

    internal var options: IpcpOptionPack = IpcpOptionPack()

    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)

        options = IpcpOptionPack(givenLength - length).also {
            it.read(buffer)
        }
    }

    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)

        options.write(buffer)
    }
}

internal class IpcpConfigureRequest : IpcpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_REQUEST
}

internal class IpcpConfigureAck : IpcpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_ACK
}

internal class IpcpConfigureNak : IpcpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_NAK
}

internal class IpcpConfigureReject : IpcpConfigureFrame() {
    override val code = LCP_CODE_CONFIGURE_REJECT
}

