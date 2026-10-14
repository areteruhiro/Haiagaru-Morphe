// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.client.ppp.auth

import app.morphe.extension.chmate.vpn.sstp.ControlMessage
import app.morphe.extension.chmate.vpn.sstp.Result
import app.morphe.extension.chmate.vpn.sstp.SharedBridge
import app.morphe.extension.chmate.vpn.sstp.Where
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.ChapChallenge
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.ChapFailure
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.ChapMessageFrame
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.ChapResponse
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.ChapSuccess


internal class ChapMSCHAPV2Client(bridge: SharedBridge) : ChapClient(bridge) {
    private val authClient = MSCHAPV2Client(bridge)

    override suspend fun responseChallenge(challenge: ChapChallenge) {
        ChapResponse().also {
            it.id = challengeID
            it.valueName = authClient.processChallenge(challenge.valueName)

            bridge.sslTerminal!!.send(it.toByteBuffer())
        }
    }

    override suspend fun processResult(result: ChapMessageFrame) {
        when (result) {
            is ChapSuccess -> {
                if (authClient.verifyAuthenticator(result.message)) {
                    authClient.prepareHlak()

                    bridge.controlMailbox.send(
                        ControlMessage(Where.CHAP, Result.PROCEEDED)
                    )
                } else {
                    bridge.controlMailbox.send(
                        ControlMessage(Where.MSCHAPV2, Result.ERR_VERIFICATION_FAILED)
                    )
                }
            }

            is ChapFailure -> {
                bridge.controlMailbox.send(
                    ControlMessage(Where.MSCHAPV2, Result.ERR_AUTHENTICATION_FAILED)
                )
            }
        }
    }
}

