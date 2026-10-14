// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.client.ppp.auth

import app.morphe.extension.chmate.vpn.sstp.ControlMessage
import app.morphe.extension.chmate.vpn.sstp.Result
import app.morphe.extension.chmate.vpn.sstp.SharedBridge
import app.morphe.extension.chmate.vpn.sstp.Where
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.PAPAuthenticateRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.PAPFrame
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.PAP_CODE_AUTHENTICATE_ACK
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.PAP_CODE_AUTHENTICATE_NAK
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


internal class PAPClient(private val bridge: SharedBridge) {
    internal val mailbox = Channel<PAPFrame>(Channel.BUFFERED)
    private var jobAuth: Job? = null

    internal fun launchJobAuth() {
        jobAuth = bridge.service.scope.launch(bridge.handler) {
            val currentID = bridge.allocateNewFrameID()

            sendPAPRequest(currentID)

            while (isActive) {
                val received = mailbox.receive()

                if (received.id != currentID) continue

                when (received.code) {
                    PAP_CODE_AUTHENTICATE_ACK -> {
                        bridge.controlMailbox.send(
                            ControlMessage(Where.PAP, Result.PROCEEDED)
                        )
                    }

                    PAP_CODE_AUTHENTICATE_NAK -> {
                        bridge.controlMailbox.send(
                            ControlMessage(Where.PAP, Result.ERR_AUTHENTICATION_FAILED)
                        )
                    }
                }
            }
        }
    }

    private suspend fun sendPAPRequest(id: Byte) {
        PAPAuthenticateRequest().also {
            it.id = id
            it.idFiled = bridge.HOME_USERNAME.toByteArray(Charsets.US_ASCII)
            it.passwordFiled = bridge.HOME_PASSWORD.toByteArray(Charsets.US_ASCII)

            bridge.sslTerminal!!.send(it.toByteBuffer())
        }
    }

    internal fun cancel() {
        jobAuth?.cancel()
        mailbox.close()
    }
}

