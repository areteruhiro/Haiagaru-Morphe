// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.unit.ppp.option

import app.morphe.extension.chmate.vpn.sstp.extension.probeByte
import java.nio.ByteBuffer


internal const val OPTION_TYPE_IPv6CP_IDENTIFIER: Byte = 0x01

internal class Ipv6cpIdentifierOption : Option() {
    override val type = OPTION_TYPE_IPv6CP_IDENTIFIER
    internal val identifier = ByteArray(8)
    override val length = headerSize + identifier.size

    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)

        buffer.get(identifier)
    }

    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)

        buffer.put(identifier)
    }
}

internal class Ipv6cpOptionPack(givenLength: Int = 0) : OptionPack(givenLength) {
    internal var identifierOption: Ipv6cpIdentifierOption? = null

    override val knownOptions: List<Option>
        get() = mutableListOf<Option>().also { options ->
            identifierOption?.also { options.add(it) }
        }

    override fun retrieveOption(buffer: ByteBuffer): Option {
        val option = when (val type = buffer.probeByte(0)) {
            OPTION_TYPE_IPv6CP_IDENTIFIER -> Ipv6cpIdentifierOption().also { identifierOption = it }

            else -> UnknownOption(type)
        }

        option.read(buffer)

        return option
    }
}

