// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.client.ppp

import app.morphe.extension.chmate.vpn.sstp.ControlMessage
import app.morphe.extension.chmate.vpn.sstp.DEFAULT_MRU
import app.morphe.extension.chmate.vpn.sstp.MIN_MRU
import app.morphe.extension.chmate.vpn.sstp.Result
import app.morphe.extension.chmate.vpn.sstp.SharedBridge
import app.morphe.extension.chmate.vpn.sstp.Where
import app.morphe.extension.chmate.vpn.sstp.preference.AUTH_PROTOCOL_EAP_MSCHAPv2
import app.morphe.extension.chmate.vpn.sstp.preference.AUTH_PROTOCOL_MSCHAPv2
import app.morphe.extension.chmate.vpn.sstp.preference.AUTH_PROTOCOl_PAP
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPConfigureAck
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPConfigureFrame
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPConfigureNak
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPConfigureReject
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPConfigureRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.PPP_PROTOCOL_CHAP
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.PPP_PROTOCOL_EAP
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.PPP_PROTOCOL_PAP
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.option.AuthOption
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.option.CHAP_ALGORITHM_MSCHAPv2
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.option.LCPOptionPack
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.option.MRUOption
import kotlin.math.max
import kotlin.math.min


internal class LCPClient(bridge: SharedBridge) : ConfigClient<LCPConfigureFrame>(Where.LCP, bridge) {
    private var isMruRejected = false

    override fun tryCreateServerReject(request: LCPConfigureFrame): LCPConfigureFrame? {
        val reject = LCPOptionPack()

        if (request.options.unknownOptions.isNotEmpty()) {
            reject.unknownOptions = request.options.unknownOptions
        }

        return if (reject.allOptions.isNotEmpty()) {
            LCPConfigureReject().also {
                it.id = request.id
                it.options = reject
                it.options.order = request.options.order
            }
        } else null
    }

    override fun tryCreateServerNak(request: LCPConfigureFrame): LCPConfigureFrame? {
        val nak = LCPOptionPack()

        val serverMru = request.options.mruOption?.unitSize ?: DEFAULT_MRU
        if (serverMru < bridge.PPP_MTU) {
            nak.mruOption = MRUOption().also { it.unitSize = bridge.PPP_MTU }
        }


        val serverAuth = request.options.authOption
        var isAuthAcknowledged = false

        when (serverAuth?.protocol) {
            PPP_PROTOCOL_EAP -> {
                if (bridge.isEnabled(AUTH_PROTOCOL_EAP_MSCHAPv2)) {
                    bridge.currentAuth = AUTH_PROTOCOL_EAP_MSCHAPv2
                    isAuthAcknowledged = true
                }
            }

            PPP_PROTOCOL_CHAP -> {
                if (serverAuth.holder.size != 1) {
                    throw NotImplementedError(serverAuth.holder.size.toString())
                }

                if (serverAuth.holder[0] == CHAP_ALGORITHM_MSCHAPv2 && bridge.isEnabled(AUTH_PROTOCOL_MSCHAPv2)) {
                    bridge.currentAuth = AUTH_PROTOCOL_MSCHAPv2
                    isAuthAcknowledged = true
                }
            }

            PPP_PROTOCOL_PAP -> {
                if (bridge.isEnabled(AUTH_PROTOCOl_PAP)) {
                    bridge.currentAuth = AUTH_PROTOCOl_PAP
                    isAuthAcknowledged = true
                }
            }
        }

        if (!isAuthAcknowledged) {
            val authOption = AuthOption()
            when {
                bridge.isEnabled(AUTH_PROTOCOL_EAP_MSCHAPv2) -> {
                    authOption.protocol = PPP_PROTOCOL_EAP
                }

                bridge.isEnabled(AUTH_PROTOCOL_MSCHAPv2) -> {
                    authOption.protocol = PPP_PROTOCOL_CHAP
                    authOption.holder = ByteArray(1) { CHAP_ALGORITHM_MSCHAPv2 }
                }

                bridge.isEnabled(AUTH_PROTOCOl_PAP) -> {
                    authOption.protocol = PPP_PROTOCOL_PAP
                }

                else -> throw NotImplementedError()
            }

            nak.authOption = authOption
        }


        return if (nak.allOptions.isNotEmpty()) {
            LCPConfigureNak().also {
                it.id = request.id
                it.options = nak
                it.options.order = request.options.order
            }
        } else null
    }

    override fun createServerAck(request: LCPConfigureFrame): LCPConfigureFrame {
        return LCPConfigureAck().also {
            it.id = request.id
            it.options = request.options
        }
    }


    override fun createClientRequest(): LCPConfigureFrame {
        val request = LCPConfigureRequest()

        if (!isMruRejected) {
            request.options.mruOption = MRUOption().also { it.unitSize = bridge.currentMRU }
        }

        return request
    }

    override suspend fun tryAcceptClientNak(nak: LCPConfigureFrame) {
        nak.options.mruOption?.also {
            bridge.currentMRU = max(min(it.unitSize, bridge.PPP_MRU), MIN_MRU)
        }
    }

    override suspend fun tryAcceptClientReject(reject: LCPConfigureFrame) {
        reject.options.mruOption?.also {
            isMruRejected = true

            if (DEFAULT_MRU > bridge.PPP_MRU) {
                bridge.controlMailbox.send(
                    ControlMessage(Where.LCP_MRU, Result.ERR_OPTION_REJECTED)
                )
            }
        }

        reject.options.authOption?.also {
            bridge.controlMailbox.send(ControlMessage(Where.LCP_AUTH, Result.ERR_OPTION_REJECTED))
        }
    }
}

