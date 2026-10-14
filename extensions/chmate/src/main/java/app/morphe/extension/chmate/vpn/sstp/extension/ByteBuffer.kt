// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.extension

import java.nio.ByteBuffer


internal fun ByteBuffer.move(diff: Int) {
    position(position() + diff)
}

internal fun ByteBuffer.padZeroByte(size: Int) {
    repeat(size) { put(0) }
}

internal fun ByteBuffer.probeByte(diff: Int): Byte {
    return this.get(this.position() + diff)
}

internal fun ByteBuffer.probeShort(diff: Int): Short {
    return this.getShort(this.position() + diff)
}

internal val ByteBuffer.capacityAfterLimit: Int
    get() = this.capacity() - this.limit()

internal fun ByteBuffer.slide() {
    val remaining = this.remaining()

    this.array().also {
        it.copyInto(it, 0, this.position(), this.limit())
    }

    this.position(0)
    this.limit(remaining)
}

