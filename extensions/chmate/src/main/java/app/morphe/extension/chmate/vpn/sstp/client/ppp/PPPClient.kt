// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.client.ppp

import app.morphe.extension.chmate.vpn.sstp.ControlMessage
import app.morphe.extension.chmate.vpn.sstp.Result
import app.morphe.extension.chmate.vpn.sstp.SharedBridge
import app.morphe.extension.chmate.vpn.sstp.Where
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.Frame
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPCodeReject
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPEchoReply
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPEchoRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPProtocolReject
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPTerminalAck
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPTerminalRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LcpDiscardRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


internal class PPPClient(val bridge: SharedBridge) {
    internal val mailbox = Channel<Frame>(Channel.BUFFERED)

    private var jobControl: Job? = null

    internal fun launchJobControl() {
        jobControl = bridge.service.scope.launch(bridge.handler) {
            while (isActive) {
                when (val received = mailbox.receive()) {
                    is LCPEchoRequest -> {
                        LCPEchoReply().also {
                            it.id = received.id
                            it.holder = "Abura Mashi Mashi".toByteArray(Charsets.US_ASCII)
                            bridge.sslTerminal!!.send(it.toByteBuffer())
                        }
                    }

                    is LCPEchoReply -> { }

                    is LcpDiscardRequest -> { }

                    is LCPTerminalRequest -> {
                        LCPTerminalAck().also {
                            it.id = received.id
                            bridge.sslTerminal!!.send(it.toByteBuffer())
                        }

                        bridge.controlMailbox.send(
                            ControlMessage(Where.PPP, Result.ERR_TERMINATE_REQUESTED)
                        )
                    }

                    is LCPProtocolReject -> {
                        bridge.controlMailbox.send(
                            ControlMessage(Where.PPP, Result.ERR_PROTOCOL_REJECTED)
                        )
                    }

                    is LCPCodeReject -> {
                        bridge.controlMailbox.send(
                            ControlMessage(Where.PPP, Result.ERR_CODE_REJECTED)
                        )
                    }

                    else -> {
                        bridge.controlMailbox.send(
                            ControlMessage(Where.PPP, Result.ERR_UNEXPECTED_MESSAGE)
                        )
                    }
                }
            }
        }
    }

    internal fun cancel() {
        jobControl?.cancel()
        mailbox.close()
    }
}

