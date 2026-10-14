// Adapted from Open-SSTP-Client 5f97511c3afff7dcf76b763882e0e29207ad1a36.
// Copyright (c) 2019 KOBAYASHI Ittoku. MIT; see third-party/open-sstp/LICENSE.
package app.morphe.extension.chmate.vpn.sstp

import app.morphe.extension.chmate.vpn.sstp.preference.OscPrefKey
import app.morphe.extension.chmate.vpn.sstp.preference.accessor.getBooleanPrefValue
import app.morphe.extension.chmate.vpn.sstp.preference.accessor.getIntPrefValue
import app.morphe.extension.chmate.vpn.sstp.preference.accessor.getSetPrefValue
import app.morphe.extension.chmate.vpn.sstp.preference.accessor.getStringPrefValue
import app.morphe.extension.chmate.vpn.sstp.service.SstpSessionHost
import app.morphe.extension.chmate.vpn.sstp.terminal.IPTerminal
import app.morphe.extension.chmate.vpn.sstp.terminal.SSLTerminal
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID


internal enum class Where {
    CERT,
    CERT_PATH,
    SSL,
    PROXY,
    SSTP_DATA,
    SSTP_CONTROL,
    SSTP_REQUEST,
    SSTP_HASH,
    PPP,
    PAP,
    CHAP,
    MSCHAPV2,
    EAP,
    LCP,
    LCP_MRU,
    LCP_AUTH,
    IPCP,
    IPCP_IP,
    IPV6CP,
    IPV6CP_IDENTIFIER,
    IP,
    IPv4,
    IPv6,
    ROUTE,
    INCOMING,
    OUTGOING,
}

internal data class ControlMessage(
    val from: Where,
    val result: Result,
    val supplement: String? = null
)

internal enum class Result {
    PROCEEDED,

    // common errors
    ERR_TIMEOUT,
    ERR_COUNT_EXHAUSTED,
    ERR_UNKNOWN_TYPE, // the data cannot be parsed
    ERR_UNEXPECTED_MESSAGE, // the data can be parsed, but it's received in the wrong time
    ERR_PARSING_FAILED,
    ERR_VERIFICATION_FAILED,

    // for SSTP
    ERR_NEGATIVE_ACKNOWLEDGED,
    ERR_ABORT_REQUESTED,
    ERR_DISCONNECT_REQUESTED,

    // for PPP
    ERR_TERMINATE_REQUESTED,
    ERR_PROTOCOL_REJECTED,
    ERR_CODE_REJECTED,
    ERR_AUTHENTICATION_FAILED,
    ERR_ADDRESS_REJECTED,
    ERR_OPTION_REJECTED,

    // for IP
    ERR_INVALID_ADDRESS,

    // for INCOMING
    ERR_INVALID_PACKET_SIZE,
}

internal class SharedBridge(internal val service: SstpSessionHost) {
    internal val prefs = service.prefs
    internal lateinit var handler: CoroutineExceptionHandler

    internal val controlMailbox = Channel<ControlMessage>(Channel.BUFFERED)

    internal var sslTerminal: SSLTerminal? = null
    internal var ipTerminal: IPTerminal? = null

    internal val HOME_USERNAME = getStringPrefValue(OscPrefKey.HOME_USERNAME, prefs)
    internal val HOME_PASSWORD = getStringPrefValue(OscPrefKey.HOME_PASSWORD, prefs)
    internal val PPP_MRU = getIntPrefValue(OscPrefKey.PPP_MRU, prefs)
    internal val PPP_MTU = getIntPrefValue(OscPrefKey.PPP_MTU, prefs)
    internal val PPP_AUTH_PROTOCOLS = getSetPrefValue(OscPrefKey.PPP_AUTH_PROTOCOLS, prefs)
    internal val PPP_IPv4_ENABLED = getBooleanPrefValue(OscPrefKey.PPP_IPv4_ENABLED, prefs)
    internal val PPP_IPv6_ENABLED = getBooleanPrefValue(OscPrefKey.PPP_IPv6_ENABLED, prefs)

    internal var hlak: ByteArray? = null
    internal val nonce = ByteArray(32)
    internal val guid = UUID.randomUUID().toString()
    internal var hashProtocol: Byte = 0

    private val mutex = Mutex()
    private var frameID = -1

    internal var currentMRU = PPP_MRU
    internal var currentAuth = ""
    internal val currentIPv4 = ByteArray(4)
    internal val currentIPv6 = ByteArray(8)
    internal val currentProposedDNS = ByteArray(4)

    internal fun isEnabled(authProtocol: String): Boolean {
        return authProtocol in PPP_AUTH_PROTOCOLS
    }

    internal fun attachSSLTerminal() {
        sslTerminal = SSLTerminal(this)
    }

    internal fun attachIPTerminal() {
        ipTerminal = IPTerminal(this)
    }

    internal suspend fun allocateNewFrameID(): Byte {
        mutex.withLock {
            frameID += 1
            return frameID.toByte()
        }
    }
}


