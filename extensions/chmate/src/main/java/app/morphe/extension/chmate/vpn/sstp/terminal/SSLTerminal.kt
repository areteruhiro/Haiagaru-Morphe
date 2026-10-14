package app.morphe.extension.chmate.vpn.sstp.terminal

import app.morphe.extension.chmate.vpn.sstp.ControlMessage
import app.morphe.extension.chmate.vpn.sstp.Result
import app.morphe.extension.chmate.vpn.sstp.SharedBridge
import app.morphe.extension.chmate.vpn.sstp.Where
import app.morphe.extension.chmate.vpn.sstp.extension.slide
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManagerFactory

internal const val SSL_REQUEST_INTERVAL = 15000L

/** Strict JSSE TLS: pinned official-profile trust anchor plus HTTPS hostname verification. */
internal class SSLTerminal(private val bridge: SharedBridge) {
    private val sendMutex = Mutex()
    @Volatile private var rawSocket: Socket? = null
    @Volatile private var socket: SSLSocket? = null
    @Volatile private var closed = false

    suspend fun initialize() {
        bridge.service.scope.launch(bridge.handler) {
            val host = bridge.service
            if (!host.address.matches(Regex("[0-9.]+")) || !host.hostname.matches(Regex("[A-Za-z0-9-]+\\.opengw\\.net"))
                    || host.port !in 1..65535) throw IOException("IMAGE_VPN_INVALID_SERVER")
            val certificate = CertificateFactory.getInstance("X.509")
                .generateCertificate(ByteArrayInputStream(host.trustAnchorPem.toByteArray(Charsets.US_ASCII))) as X509Certificate
            certificate.checkValidity()
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null); setCertificateEntry("official-vpngate-profile", certificate)
            }
            val trust = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply { init(keyStore) }
            val context = SSLContext.getInstance("TLS").apply { init(null, trust.trustManagers, null) }
            val transport = Socket()
            synchronized(this@SSLTerminal) {
                if (closed) { transport.close(); throw IOException("IMAGE_VPN_DISCONNECTED") }
                rawSocket = transport
            }
            // Endpoint IP comes from the TLS-authenticated official catalogue, not target-image DNS.
            transport.connect(InetSocketAddress(InetAddress.getByName(host.address), host.port), 10000)
            transport.soTimeout = 10000
            val tls = context.socketFactory.createSocket(transport, host.hostname, host.port, true) as SSLSocket
            synchronized(this@SSLTerminal) {
                if (closed) { tls.close(); throw IOException("IMAGE_VPN_DISCONNECTED") }
                socket = tls
            }
            tls.enabledProtocols = tls.supportedProtocols.filter { it == "TLSv1.2" || it == "TLSv1.3" }.toTypedArray()
            tls.sslParameters = tls.sslParameters.apply { endpointIdentificationAlgorithm = "HTTPS" }
            tls.startHandshake()
            val request = "SSTP_DUPLEX_POST /sra_{BA195980-CD49-458b-9E23-C84EE0ADCD75}/ HTTP/1.1\r\n" +
                "Host: ${host.hostname}\r\nContent-Length: 18446744073709551615\r\n" +
                "SSTPCORRELATIONID: {${bridge.guid}}\r\n\r\n"
            tls.outputStream.write(request.toByteArray(Charsets.US_ASCII)); tls.outputStream.flush()
            val header = ByteArrayOutputStream()
            var suffix = 0
            var headerComplete = false
            while (header.size() < 16384 && isActive) {
                val next = tls.inputStream.read()
                if (next == -1) throw EOFException("IMAGE_VPN_SSTP_HTTP_EOF")
                header.write(next)
                suffix = (suffix shl 8) or next
                if (suffix == 0x0d0a0d0a) { headerComplete = true; break }
            }
            val status = header.toString("US-ASCII").substringBefore("\r\n")
            if (!headerComplete || !status.matches(Regex("HTTP/1\\.[01] 200(?: .*)?"))) {
                throw IOException("IMAGE_VPN_SSTP_HTTP_REJECTED")
            }
            tls.soTimeout = 1000
            bridge.controlMailbox.send(ControlMessage(Where.SSL, Result.PROCEEDED))
        }
    }

    suspend fun send(buffer: ByteBuffer) {
        sendMutex.withLock {
            val tls = socket ?: throw IOException("IMAGE_VPN_DISCONNECTED")
            val data = ByteArray(buffer.remaining()); buffer.get(data)
            tls.outputStream.write(data); tls.outputStream.flush()
        }
    }

    fun receive(buffer: ByteBuffer) {
        val tls = socket ?: throw IOException("IMAGE_VPN_DISCONNECTED")
        buffer.slide()
        val start = buffer.limit()
        if (start == buffer.capacity()) throw IOException("IMAGE_VPN_SSTP_PACKET_TOO_LARGE")
        try {
            val count = tls.inputStream.read(buffer.array(), start, buffer.capacity() - start)
            if (count < 0) throw EOFException("IMAGE_VPN_SSTP_EOF")
            buffer.limit(start + count)
        } catch (_: SocketTimeoutException) { }
    }

    fun getSession(): SSLSession = socket?.session ?: throw IOException("IMAGE_VPN_DISCONNECTED")
    fun getServerCertificate(): ByteArray = getSession().peerCertificates[0].encoded
    fun getApplicationBufferSize(): Int = 32768
    @Synchronized fun close() {
        closed = true
        try { socket?.close() } finally { rawSocket?.close(); socket = null; rawSocket = null }
    }
}
