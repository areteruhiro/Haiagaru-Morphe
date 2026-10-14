// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.unit.ppp.option

import app.morphe.extension.chmate.vpn.sstp.extension.probeByte
import java.nio.ByteBuffer


internal const val OPTION_TYPE_IPCP_IP: Byte = 0x03
internal const val OPTION_TYPE_IPCP_DNS = 0x81.toByte()


internal class IpcpAddressOption(override val type: Byte) : Option() {
    internal val address = ByteArray(4)
    override val length = headerSize + address.size

    override fun read(buffer: ByteBuffer) {
        readHeader(buffer)

        buffer.get(address)
    }

    override fun write(buffer: ByteBuffer) {
        writeHeader(buffer)

        buffer.put(address)
    }
}

internal class IpcpOptionPack(givenLength: Int = 0) : OptionPack(givenLength) {
    internal var ipOption: IpcpAddressOption? = null
    internal var dnsOption: IpcpAddressOption? = null

    override val knownOptions: List<Option>
        get() = mutableListOf<Option>().also { options ->
            ipOption?.also { options.add(it) }
            dnsOption?.also { options.add(it) }
        }

    override fun retrieveOption(buffer: ByteBuffer): Option {
        val option = when (val type = buffer.probeByte(0)) {
            OPTION_TYPE_IPCP_IP -> IpcpAddressOption(type).also { ipOption = it }

            OPTION_TYPE_IPCP_DNS -> IpcpAddressOption(type).also { dnsOption = it }

            else -> UnknownOption(type)
        }

        option.read(buffer)

        return option
    }
}

