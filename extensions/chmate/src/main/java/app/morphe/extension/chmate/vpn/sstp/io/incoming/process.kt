// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.io.incoming

import app.morphe.extension.chmate.vpn.sstp.ControlMessage
import app.morphe.extension.chmate.vpn.sstp.Result
import app.morphe.extension.chmate.vpn.sstp.Where
import app.morphe.extension.chmate.vpn.sstp.extension.move
import app.morphe.extension.chmate.vpn.sstp.unit.DataUnit
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.IpcpConfigureAck
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.IpcpConfigureNak
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.IpcpConfigureReject
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.IpcpConfigureRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.Ipv6cpConfigureAck
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.Ipv6cpConfigureNak
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.Ipv6cpConfigureReject
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.Ipv6cpConfigureRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPCodeReject
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPConfigureAck
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPConfigureNak
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPConfigureReject
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPConfigureRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPEchoReply
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPEchoRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPProtocolReject
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPTerminalAck
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCPTerminalRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_CODE_REJECT
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_CONFIGURE_ACK
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_CONFIGURE_NAK
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_CONFIGURE_REJECT
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_CONFIGURE_REQUEST
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_DISCARD_REQUEST
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_ECHO_REPLY
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_ECHO_REQUEST
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_PROTOCOL_REJECT
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_TERMINATE_ACK
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LCP_CODE_TERMINATE_REQUEST
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.LcpDiscardRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.CHAP_CODE_CHALLENGE
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.CHAP_CODE_FAILURE
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.CHAP_CODE_RESPONSE
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.CHAP_CODE_SUCCESS
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.ChapChallenge
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.ChapFailure
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.ChapResponse
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.ChapSuccess
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.EAPFailure
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.EAPRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.EAPResponse
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.EAPSuccess
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.EAP_CODE_FAILURE
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.EAP_CODE_REQUEST
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.EAP_CODE_RESPONSE
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.EAP_CODE_SUCCESS
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.PAPAuthenticateAck
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.PAPAuthenticateNak
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.PAPAuthenticateRequest
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.PAP_CODE_AUTHENTICATE_ACK
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.PAP_CODE_AUTHENTICATE_NAK
import app.morphe.extension.chmate.vpn.sstp.unit.ppp.auth.PAP_CODE_AUTHENTICATE_REQUEST
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_CALL_ABORT
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_CALL_CONNECTED
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_CALL_CONNECT_ACK
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_CALL_CONNECT_NAK
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_CALL_CONNECT_REQUEST
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_ECHO_REQUEST
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_ECHO_RESPONSE
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SstpCallAbort
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SstpCallConnectAck
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SstpCallConnectNak
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SstpCallConnectRequest
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SstpCallConnected
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SstpCallDisconnect
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SstpCallDisconnectAck
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SstpEchoRequest
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SstpEchoResponse
import java.nio.ByteBuffer


private suspend fun IncomingManager.tryReadDataUnit(unit: DataUnit, buffer: ByteBuffer): Exception? {
    try {
        unit.read(buffer)
    } catch (e: Exception) { // need to save packet log
        bridge.controlMailbox.send(
            ControlMessage(Where.INCOMING, Result.ERR_PARSING_FAILED)
        )

        return e
    }

    return null
}

internal suspend fun IncomingManager.processControlPacket(type: Short, buffer: ByteBuffer): Boolean {
    val packet = when (type) {
        SSTP_MESSAGE_TYPE_CALL_CONNECT_REQUEST -> SstpCallConnectRequest()
        SSTP_MESSAGE_TYPE_CALL_CONNECT_ACK -> SstpCallConnectAck()
        SSTP_MESSAGE_TYPE_CALL_CONNECT_NAK -> SstpCallConnectNak()
        SSTP_MESSAGE_TYPE_CALL_CONNECTED -> SstpCallConnected()
        SSTP_MESSAGE_TYPE_CALL_ABORT -> SstpCallAbort()
        SSTP_MESSAGE_TYPE_CALL_DISCONNECT -> SstpCallDisconnect()
        SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK -> SstpCallDisconnectAck()
        SSTP_MESSAGE_TYPE_ECHO_REQUEST -> SstpEchoRequest()
        SSTP_MESSAGE_TYPE_ECHO_RESPONSE -> SstpEchoResponse()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.SSTP_CONTROL, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(packet, buffer)?.also {
        return false
    }

    sstpMailbox?.send(packet)

    return true
}

internal suspend fun IncomingManager.processLcpFrame(code: Byte, buffer: ByteBuffer): Boolean {
    if (code in 1..4) {
        val configureFrame = when (code) {
            LCP_CODE_CONFIGURE_REQUEST -> LCPConfigureRequest()
            LCP_CODE_CONFIGURE_ACK -> LCPConfigureAck()
            LCP_CODE_CONFIGURE_NAK -> LCPConfigureNak()
            LCP_CODE_CONFIGURE_REJECT -> LCPConfigureReject()
            else -> throw NotImplementedError(code.toString())
        }

        tryReadDataUnit(configureFrame, buffer)?.also {
            return false
        }

        lcpMailbox?.send(configureFrame)
        return true
    }

    if (code in 5..11) {
        val frame = when (code) {
            LCP_CODE_TERMINATE_REQUEST -> LCPTerminalRequest()
            LCP_CODE_TERMINATE_ACK -> LCPTerminalAck()
            LCP_CODE_CODE_REJECT -> LCPCodeReject()
            LCP_CODE_PROTOCOL_REJECT -> LCPProtocolReject()
            LCP_CODE_ECHO_REQUEST -> LCPEchoRequest()
            LCP_CODE_ECHO_REPLY -> LCPEchoReply()
            LCP_CODE_DISCARD_REQUEST -> LcpDiscardRequest()
            else -> throw NotImplementedError(code.toString())
        }

        tryReadDataUnit(frame, buffer)?.also {
            return false
        }

        pppMailbox?.send(frame)
        return true
    }

    bridge.controlMailbox.send(
        ControlMessage(Where.LCP, Result.ERR_UNKNOWN_TYPE)
    )

    return false
}

internal suspend fun IncomingManager.processPAPFrame(code: Byte, buffer: ByteBuffer): Boolean {
    val frame = when (code) {
        PAP_CODE_AUTHENTICATE_REQUEST -> PAPAuthenticateRequest()
        PAP_CODE_AUTHENTICATE_ACK -> PAPAuthenticateAck()
        PAP_CODE_AUTHENTICATE_NAK -> PAPAuthenticateNak()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.PAP, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(frame, buffer)?.also {
        return false
    }

    papMailbox?.send(frame)
    return true
}

internal suspend fun IncomingManager.processChapFrame(code: Byte, buffer: ByteBuffer): Boolean {
    val frame = when (code) {
        CHAP_CODE_CHALLENGE -> ChapChallenge()
        CHAP_CODE_RESPONSE -> ChapResponse()
        CHAP_CODE_SUCCESS -> ChapSuccess()
        CHAP_CODE_FAILURE -> ChapFailure()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.CHAP, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(frame, buffer)?.also {
        return false
    }

    chapMailbox?.send(frame)
    return true
}

internal suspend fun IncomingManager.processEAPFrame(code: Byte, buffer: ByteBuffer): Boolean {
    val frame = when (code) {
        EAP_CODE_REQUEST -> EAPRequest()
        EAP_CODE_RESPONSE -> EAPResponse()
        EAP_CODE_SUCCESS -> EAPSuccess()
        EAP_CODE_FAILURE -> EAPFailure()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.EAP, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(frame, buffer)?.also {
        return false
    }

    eapMailbox?.send(frame)
    return true
}

internal suspend fun IncomingManager.processIpcpFrame(code: Byte, buffer: ByteBuffer): Boolean {
    val frame = when (code) {
        LCP_CODE_CONFIGURE_REQUEST -> IpcpConfigureRequest()
        LCP_CODE_CONFIGURE_ACK -> IpcpConfigureAck()
        LCP_CODE_CONFIGURE_NAK -> IpcpConfigureNak()
        LCP_CODE_CONFIGURE_REJECT -> IpcpConfigureReject()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.IPCP, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(frame, buffer)?.also {
        return false
    }

    ipcpMailbox?.send(frame)
    return true
}

internal suspend fun IncomingManager.processIpv6cpFrame(code: Byte, buffer: ByteBuffer): Boolean {
    val frame = when (code) {
        LCP_CODE_CONFIGURE_REQUEST -> Ipv6cpConfigureRequest()
        LCP_CODE_CONFIGURE_ACK -> Ipv6cpConfigureAck()
        LCP_CODE_CONFIGURE_NAK -> Ipv6cpConfigureNak()
        LCP_CODE_CONFIGURE_REJECT -> Ipv6cpConfigureReject()
        else -> {
            bridge.controlMailbox.send(
                ControlMessage(Where.IPV6CP, Result.ERR_UNKNOWN_TYPE)
            )

            return false
        }
    }

    tryReadDataUnit(frame, buffer)?.also {
        return false
    }

    ipv6cpMailbox?.send(frame)
    return true
}

internal suspend fun IncomingManager.processUnknownProtocol(protocol: Short, packetSize: Int, buffer: ByteBuffer): Boolean {
    LCPProtocolReject().also {
        it.rejectedProtocol = protocol
        it.id = bridge.allocateNewFrameID()
        val infoStart = buffer.position() + 8
        val infoStop = buffer.position() + packetSize
        it.holder = buffer.array().sliceArray(infoStart until infoStop)

        bridge.sslTerminal!!.send(it.toByteBuffer())
    }

    buffer.move(packetSize)

    return true
}

internal fun IncomingManager.processIPPacket(isEnabledProtocol: Boolean, packetSize: Int, buffer: ByteBuffer) {
    if (isEnabledProtocol) {
        val start = buffer.position() + 8
        val ipPacketSize = packetSize - 8

        bridge.ipTerminal!!.writePacket(start, ipPacketSize, buffer)
    }

    buffer.move(packetSize)
}

