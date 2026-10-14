// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth

import app.morphe.extension.chmate.vpn.sstp.extension.toIntAsUByte
import app.morphe.extension.chmate.vpn.sstp.unit.DataUnit
import java.nio.ByteBuffer


internal class ChapValueNameFiled : DataUnit() {
    internal var value = ByteArray(0)
    internal var name = ByteArray(0)
    internal var givenLength = 0 // must be given before reading
    override val length: Int
        get() = 1 + value.size + name.size

    override fun read(buffer: ByteBuffer) {
        value = ByteArray(buffer.get().toIntAsUByte())
        buffer.get(value)

        name = ByteArray(givenLength - length)
        buffer.get(name)
    }

    override fun write(buffer: ByteBuffer) {
        buffer.put(value.size.toByte())
        buffer.put(value)
        buffer.put(name)
    }
}

internal class ChapMessageField : DataUnit() {
    internal var message = ByteArray(0)
    internal var givenLength = 0 // must be given before reading
    override val length: Int
        get() = message.size

    override fun read(buffer: ByteBuffer) {
        message = ByteArray(givenLength)
        buffer.get(message)
    }

    override fun write(buffer: ByteBuffer) {
        buffer.put(message)
    }
}

