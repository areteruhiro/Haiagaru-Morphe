// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.client.ppp

import app.morphe.extension.chmate.vpn.sstp.ControlMessage
import app.morphe.extension.chmate.vpn.sstp.Result
import app.morphe.extension.chmate.vpn.sstp.SharedBridge
import app.morphe.extension.chmate.vpn.sstp.Where
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.Ipv6cpConfigureAck
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.Ipv6cpConfigureFrame
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.Ipv6cpConfigureReject
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.Ipv6cpConfigureRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.option.Ipv6cpIdentifierOption
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.option.Ipv6cpOptionPack


internal class Ipv6cpClient(bridge: SharedBridge) : ConfigClient<Ipv6cpConfigureFrame>(Where.IPV6CP, bridge) {
    override fun tryCreateServerReject(request: Ipv6cpConfigureFrame): Ipv6cpConfigureFrame? {
        val reject = Ipv6cpOptionPack()

        if (request.options.unknownOptions.isNotEmpty()) {
            reject.unknownOptions = request.options.unknownOptions
        }

        return if (reject.allOptions.isNotEmpty()) {
            Ipv6cpConfigureReject().also {
                it.id = request.id
                it.options = reject
                it.options.order = request.options.order
            }
        } else null
    }

    override fun tryCreateServerNak(request: Ipv6cpConfigureFrame): Ipv6cpConfigureFrame? {
        return null
    }

    override fun createServerAck(request: Ipv6cpConfigureFrame): Ipv6cpConfigureFrame {
        return Ipv6cpConfigureAck().also {
            it.id = request.id
            it.options = request.options
        }
    }

    override fun createClientRequest(): Ipv6cpConfigureFrame {
        val request = Ipv6cpConfigureRequest()

        request.options.identifierOption = Ipv6cpIdentifierOption().also {
            bridge.currentIPv6.copyInto(it.identifier)
        }

        return request
    }

    override suspend fun tryAcceptClientNak(nak: Ipv6cpConfigureFrame) {
        nak.options.identifierOption?.also {
            it.identifier.copyInto(bridge.currentIPv6)
        }
    }

    override suspend fun tryAcceptClientReject(reject: Ipv6cpConfigureFrame) {
        reject.options.identifierOption?.also {
            bridge.controlMailbox.send(
                ControlMessage(Where.IPV6CP_IDENTIFIER, Result.ERR_OPTION_REJECTED)
            )
        }
    }
}

