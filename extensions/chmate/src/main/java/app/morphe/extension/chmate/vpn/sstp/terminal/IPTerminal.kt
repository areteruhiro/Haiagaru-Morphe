package app.morphe.extension.chmate.vpn.sstp.terminal

import app.morphe.extension.chmate.vpn.sstp.ControlMessage
import app.morphe.extension.chmate.vpn.sstp.Result
import app.morphe.extension.chmate.vpn.sstp.SharedBridge
import app.morphe.extension.chmate.vpn.sstp.SstpImageSession
import app.morphe.extension.chmate.vpn.sstp.Where
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import java.nio.ByteBuffer

/** Private packet queues replace /dev/tun. No system VPN routes/DNS are installed. */
internal class IPTerminal(private val bridge: SharedBridge) {
    private val outgoing = Channel<ByteArray>(128)
    @Volatile private var initialized = false

    suspend fun initialize() {
        if (bridge.currentIPv4.all { it == 0.toByte() } || bridge.currentProposedDNS.all { it == 0.toByte() }) {
            bridge.controlMailbox.send(ControlMessage(Where.IP, Result.ERR_INVALID_ADDRESS))
            return
        }
        bridge.service.packets.initialize(
            InetAddress.getByAddress(bridge.currentIPv4).hostAddress!!,
            InetAddress.getByAddress(bridge.currentProposedDNS).hostAddress!!,
            bridge.PPP_MTU,
            object : SstpImageSession.PacketWriter {
                override fun writePacket(packet: ByteArray) {
                    require(packet.size in 20..bridge.PPP_MTU && packet[0].toInt().ushr(4) == 4)
                    runBlocking { outgoing.send(packet.copyOf()) }
                }
            })
        initialized = true
        bridge.controlMailbox.send(ControlMessage(Where.IP, Result.PROCEEDED))
    }

    fun writePacket(start: Int, size: Int, buffer: ByteBuffer) {
        if (initialized) bridge.service.packets.receive(buffer.array().copyOfRange(start, start + size))
    }

    suspend fun readPacket(buffer: ByteBuffer) {
        buffer.clear()
        buffer.put(outgoing.receive())
        buffer.flip()
    }

    fun close() {
        initialized = false
        outgoing.cancel()
        bridge.service.packets.close()
    }
}
