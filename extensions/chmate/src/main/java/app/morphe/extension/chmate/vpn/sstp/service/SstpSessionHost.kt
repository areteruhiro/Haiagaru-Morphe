package app.morphe.extension.chmate.vpn.sstp.service

import android.content.Context
import app.morphe.extension.chmate.vpn.sstp.SharedBridge
import app.morphe.extension.chmate.vpn.sstp.SstpImageSession
import app.morphe.extension.chmate.vpn.sstp.control.Controller
import app.morphe.extension.chmate.vpn.sstp.control.LogWriter
import app.morphe.extension.chmate.vpn.sstp.preference.OscPrefKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/** Replaces upstream's OS-wide VPN service with a process-private lifecycle. */
internal class SstpSessionHost(
    context: Context,
    val address: String,
    val hostname: String,
    val port: Int,
    val trustAnchorPem: String,
    val packets: SstpImageSession.PacketStack,
    private val onReady: () -> Unit,
    private val onClosed: (String) -> Unit
) {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val prefs = context.getSharedPreferences("haiagaru_image_vpn_engine", Context.MODE_PRIVATE)
    val logWriter: LogWriter? = null
    private val started = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    @Volatile private var error = "IMAGE_VPN_DISCONNECTED"
    private var bridge: SharedBridge? = null

    @Synchronized fun start() {
        if (!started.compareAndSet(false, true) || closed.get()) throw IOException("IMAGE_VPN_SESSION_ALREADY_STARTED")
        try {
        // No UI, user credentials, global preferences, routes or reconnection state are reused.
        if (!prefs.edit().clear()
                .putString(OscPrefKey.HOME_USERNAME.name, "vpn")
                .putString(OscPrefKey.HOME_PASSWORD.name, "vpn")
                .putBoolean(OscPrefKey.RECONNECTION_ENABLED.name, false)
                .putBoolean(OscPrefKey.PPP_IPv4_ENABLED.name, true)
                .putBoolean(OscPrefKey.PPP_IPv6_ENABLED.name, false)
                .putBoolean(OscPrefKey.DNS_DO_REQUEST_ADDRESS.name, true)
                .putString(OscPrefKey.PPP_AUTH_TIMEOUT.name, "15")
                .commit()) throw IOException("IMAGE_VPN_ENGINE_CONFIG_FAILED")
        val created = SharedBridge(this)
        bridge = created
        Controller(created).launchJobMain()
        } catch (failure: Exception) {
            notifyError("START_FAILED")
            close()
            throw IOException("IMAGE_VPN_START_FAILED", failure)
        }
    }

    @Synchronized fun onPacketsReady() { if (!closed.get()) onReady() }
    fun notifyError(message: String) { error = "IMAGE_VPN_" + message.replace(Regex("[^A-Z0-9_:]"), "_") }
    fun launchJobReconnect() { notifyError("UNEXPECTED_RECONNECT"); close() }

    @Synchronized fun close() {
        if (!closed.compareAndSet(false, true)) return
        onClosed(error)
        // Cleanup must not replace a connection error or keep the other resource alive.
        runCatching { bridge?.sslTerminal?.close() }
        runCatching { bridge?.ipTerminal?.close() ?: packets.close() }
        scope.cancel()
    }
}
