// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.unit

import java.nio.ByteBuffer


internal abstract class DataUnit {
    internal abstract val length: Int

    internal abstract fun write(buffer: ByteBuffer)

    internal abstract fun read(buffer: ByteBuffer)

    internal fun toByteBuffer(): ByteBuffer {
        val buffer = ByteBuffer.allocate(length)

        write(buffer)
        buffer.flip()

        return buffer
    }
}

