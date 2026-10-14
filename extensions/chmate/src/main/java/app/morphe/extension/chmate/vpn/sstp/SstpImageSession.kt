package app.morphe.extension.chmate.vpn.sstp

import android.content.Context
import app.morphe.extension.chmate.vpn.sstp.service.SstpSessionHost
import java.io.Closeable
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Private SSTP packet session; never starts Android VpnService or changes OS routes. */
class SstpImageSession(
    context: Context, address: String, hostname: String, port: Int, trustAnchorPem: String,
    packets: PacketStack
) : Closeable {
    interface PacketWriter { fun writePacket(packet: ByteArray) }
    interface PacketStack {
        fun initialize(address: String, dns: String, mtu: Int, writer: PacketWriter)
        fun receive(packet: ByteArray)
        fun close()
    }

    private val ready = CountDownLatch(1)
    @Volatile private var failure: String? = null
    @Volatile private var connected = false
    private val host = SstpSessionHost(context.applicationContext ?: context,
        address, hostname, port, trustAnchorPem, packets,
        onReady = { connected = true; ready.countDown() },
        onClosed = { error -> failure = error; connected = false; ready.countDown() })

    fun start() = host.start()

    /** Call only from a worker thread. Ready means PPP/IP and packet workers are running. */
    fun awaitReady(timeoutMillis: Long) {
        if (timeoutMillis !in 1000..60000) throw IOException("IMAGE_VPN_INVALID_CONNECT_TIMEOUT")
        try {
            if (!ready.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
                close()
                throw IOException("IMAGE_VPN_CONNECT_TIMEOUT")
            }
        } catch (interrupted: InterruptedException) {
            close()
            Thread.currentThread().interrupt()
            throw IOException("IMAGE_VPN_CONNECT_INTERRUPTED", interrupted)
        }
        if (!connected) throw IOException(failure ?: "IMAGE_VPN_DISCONNECTED")
    }

    fun isConnected(): Boolean = connected
    override fun close() = host.close()
}
