// Adapted from Open-SSTP-Client, revision 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp.control

import app.morphe.extension.chmate.vpn.sstp.ControlMessage
import app.morphe.extension.chmate.vpn.sstp.Result
import app.morphe.extension.chmate.vpn.sstp.SharedBridge
import app.morphe.extension.chmate.vpn.sstp.Where
import app.morphe.extension.chmate.vpn.sstp.client.SSTP_REQUEST_TIMEOUT
import app.morphe.extension.chmate.vpn.sstp.client.SstpClient
import app.morphe.extension.chmate.vpn.sstp.client.ppp.IpcpClient
import app.morphe.extension.chmate.vpn.sstp.client.ppp.Ipv6cpClient
import app.morphe.extension.chmate.vpn.sstp.client.ppp.LCPClient
import app.morphe.extension.chmate.vpn.sstp.client.ppp.PPPClient
import app.morphe.extension.chmate.vpn.sstp.client.ppp.PPP_NEGOTIATION_TIMEOUT
import app.morphe.extension.chmate.vpn.sstp.client.ppp.auth.ChapClient
import app.morphe.extension.chmate.vpn.sstp.client.ppp.auth.ChapMSCHAPV2Client
import app.morphe.extension.chmate.vpn.sstp.client.ppp.auth.EAPClient
import app.morphe.extension.chmate.vpn.sstp.client.ppp.auth.EAPMSAuthClient
import app.morphe.extension.chmate.vpn.sstp.client.ppp.auth.PAPClient
import app.morphe.extension.chmate.vpn.sstp.debug.assertAlways
import app.morphe.extension.chmate.vpn.sstp.io.OutgoingManager
import app.morphe.extension.chmate.vpn.sstp.io.incoming.IncomingManager
import app.morphe.extension.chmate.vpn.sstp.preference.AUTH_PROTOCOL_EAP_MSCHAPv2
import app.morphe.extension.chmate.vpn.sstp.preference.AUTH_PROTOCOL_MSCHAPv2
import app.morphe.extension.chmate.vpn.sstp.preference.AUTH_PROTOCOl_PAP
import app.morphe.extension.chmate.vpn.sstp.preference.OscPrefKey
import app.morphe.extension.chmate.vpn.sstp.preference.accessor.getBooleanPrefValue
import app.morphe.extension.chmate.vpn.sstp.preference.accessor.getIntPrefValue
import app.morphe.extension.chmate.vpn.sstp.preference.accessor.resetReconnectionLife
import app.morphe.extension.chmate.vpn.sstp.terminal.SSL_REQUEST_INTERVAL
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_CALL_ABORT
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT
import app.morphe.extension.chmate.vpn.sstp.unit.sstp.SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull


internal class Controller(internal val bridge: SharedBridge) {

    private var sstpClient: SstpClient? = null
    private var pppClient: PPPClient? = null
    private var incomingManager: IncomingManager? = null
    private var outgoingManager: OutgoingManager? = null

    private var lcpClient: LCPClient? = null
    private var papClient: PAPClient? = null
    private var chapClient: ChapClient? = null
    private var eapClient: EAPClient? = null
    private var ipcpClient: IpcpClient? = null
    private var ipv6cpClient: Ipv6cpClient? = null

    private var jobMain: Job? = null

    private val mutex = Mutex()

    private val isReconnectionEnabled = getBooleanPrefValue(OscPrefKey.RECONNECTION_ENABLED, bridge.prefs)
    private val isReconnectionAvailable: Boolean
        get() = getIntPrefValue(OscPrefKey.RECONNECTION_LIFE, bridge.prefs) > 0

    private fun attachHandler() {
        bridge.handler = CoroutineExceptionHandler { _, throwable ->
            if (app.morphe.extension.chmate.BuildConfig.DEBUG)
                android.util.Log.w("HaiagaruVpnEngine", "SSTP engine failure", throwable)
            kill(isReconnectionEnabled) {
                val header = "OSC: ERR_UNEXPECTED"
                bridge.service.logWriter?.report(header + "\n" + throwable.stackTraceToString())
                bridge.service.notifyError(header)
            }
        }
    }

    internal fun launchJobMain() {
        attachHandler()

        jobMain = bridge.service.scope.launch(bridge.handler) {
            bridge.attachSSLTerminal()
            bridge.attachIPTerminal()


            bridge.sslTerminal!!.initialize()
            if (!expectProceeded(Where.SSL, SSL_REQUEST_INTERVAL)) {
                return@launch
            }


            IncomingManager(bridge).also {
                it.launchJobMain()
                incomingManager = it
            }


            SstpClient(bridge).also {
                sstpClient = it
                incomingManager!!.registerMailbox(it)
                it.launchJobRequest()

                if (!expectProceeded(Where.SSTP_REQUEST, SSTP_REQUEST_TIMEOUT)) {
                    return@launch
                }

                sstpClient!!.launchJobControl()
            }


            PPPClient(bridge).also {
                pppClient = it
                incomingManager!!.registerMailbox(it)
                it.launchJobControl()
            }


            LCPClient(bridge).also {
                incomingManager!!.registerMailbox(it)
                it.launchJobNegotiation()

                if (!expectProceeded(Where.LCP, PPP_NEGOTIATION_TIMEOUT)) {
                    return@launch
                }

                incomingManager!!.unregisterMailbox(it)
            }


            val authTimeout = getIntPrefValue(OscPrefKey.PPP_AUTH_TIMEOUT, bridge.prefs) * 1000L
            when (bridge.currentAuth) {
                AUTH_PROTOCOl_PAP -> PAPClient(bridge).also {
                    incomingManager!!.registerMailbox(it)
                    it.launchJobAuth()

                    if (!expectProceeded(Where.PAP, authTimeout)) {
                        return@launch
                    }

                    incomingManager!!.unregisterMailbox(it)
                }

                AUTH_PROTOCOL_MSCHAPv2 -> ChapMSCHAPV2Client(bridge).also {
                    chapClient = it
                    incomingManager!!.registerMailbox(it)
                    it.launchJobAuth()

                    if (!expectProceeded(Where.CHAP, authTimeout)) {
                        return@launch
                    }
                }

                AUTH_PROTOCOL_EAP_MSCHAPv2 -> EAPMSAuthClient(bridge).also {
                    eapClient = it
                    incomingManager!!.registerMailbox(it)
                    it.launchJobAuth()

                    if (!expectProceeded(Where.EAP, authTimeout)) {
                        return@launch
                    }
                }

                else -> throw NotImplementedError(bridge.currentAuth)
            }


            sstpClient!!.sendCallConnected()


            if (bridge.PPP_IPv4_ENABLED) {
                IpcpClient(bridge).also {
                    incomingManager!!.registerMailbox(it)
                    it.launchJobNegotiation()

                    if (!expectProceeded(Where.IPCP, PPP_NEGOTIATION_TIMEOUT)) {
                        return@launch
                    }

                    incomingManager!!.unregisterMailbox(it)
                }
            }


            if (bridge.PPP_IPv6_ENABLED) {
                Ipv6cpClient(bridge).also {
                    incomingManager!!.registerMailbox(it)
                    it.launchJobNegotiation()

                    if (!expectProceeded(Where.IPV6CP, PPP_NEGOTIATION_TIMEOUT)) {
                        return@launch
                    }

                    incomingManager!!.unregisterMailbox(it)
                }
            }


            bridge.ipTerminal!!.initialize()
            if (!expectProceeded(Where.IP, null)) {
                return@launch
            }


            OutgoingManager(bridge).also {
                it.launchJobMain()
                outgoingManager = it
            }

            bridge.service.onPacketsReady()


            // No OS VPN observer: this is an image-only private packet tunnel.

            if (isReconnectionEnabled) {
                resetReconnectionLife(bridge.prefs)
            }


            expectProceeded(Where.SSTP_CONTROL, null) // wait ERR_ message until disconnection
        }
    }

    private suspend fun expectProceeded(where: Where, timeout: Long?): Boolean {
        val received = if (timeout != null) {
            withTimeoutOrNull(timeout) {
                bridge.controlMailbox.receive()
            } ?: ControlMessage(where, Result.ERR_TIMEOUT)
        } else {
            bridge.controlMailbox.receive()
        }

        if (received.result == Result.PROCEEDED) {
            assertAlways(received.from == where)

            return true
        }

        val lastPacketType = if (received.result == Result.ERR_DISCONNECT_REQUESTED) {
            SSTP_MESSAGE_TYPE_CALL_DISCONNECT_ACK
        } else {
            SSTP_MESSAGE_TYPE_CALL_ABORT
        }

        kill(isReconnectionEnabled) {
            sstpClient?.sendLastPacket(lastPacketType)

            val header = "${received.from.name}: ${received.result.name}"
            var log = header
            if (received.supplement != null) {
                log += "\n${received.supplement}"
            }

            bridge.service.logWriter?.report(log)
            bridge.service.notifyError(header)
        }

        return false
    }

    internal fun disconnect() { // use if the user want to normally disconnect
        kill(false) {
            sstpClient?.sendLastPacket(SSTP_MESSAGE_TYPE_CALL_DISCONNECT)
        }
    }

    internal fun kill(isReconnectionRequested: Boolean, cleanup: (suspend () -> Unit)?) {
        if (!mutex.tryLock()) return

        bridge.service.scope.launch {
            jobMain?.cancel()
            cancelClients()

            try {
                cleanup?.invoke()
            } finally {
                try { closeTerminals() } finally {
                    if (isReconnectionRequested && isReconnectionAvailable) {
                        bridge.service.launchJobReconnect()
                    } else {
                        bridge.service.close()
                    }
                }
            }
        }
    }

    private fun cancelClients() {
        lcpClient?.cancel()
        papClient?.cancel()
        chapClient?.cancel()
        eapClient?.cancel()
        ipcpClient?.cancel()
        ipv6cpClient?.cancel()
        sstpClient?.cancel()
        pppClient?.cancel()
        incomingManager?.cancel()
        outgoingManager?.cancel()
    }

    private fun closeTerminals() {
        bridge.sslTerminal?.close()
        bridge.ipTerminal?.close()
    }
}

