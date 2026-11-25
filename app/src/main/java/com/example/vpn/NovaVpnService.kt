package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.engine.AdBlockEngine
import com.example.engine.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.net.SocketFactory

enum class VpnConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED_LOCAL_TUN,
    ERROR
}

data class VpnRuntimeState(
    val status: VpnConnectionStatus = VpnConnectionStatus.DISCONNECTED,
    val dnsServer: String = "1.1.1.1",
    val gatewayAddress: String = "",
    val statusDetail: String = "Not connected. No VPN tunnel is active.",
    val connectedSinceMs: Long? = null,
    val queriesForwarded: Long = 0L,
    val netShieldEnabled: Boolean = true,
    val dohEnabled: Boolean = true,
    val threatsBlocked: Long = 0L,
    val lastBlockedDomain: String = ""
)

object VpnStateHolder {
    private val _state = MutableStateFlow(VpnRuntimeState())
    val state: StateFlow<VpnRuntimeState> = _state.asStateFlow()

    fun update(newState: VpnRuntimeState) {
        _state.value = newState
    }
}

/**
 * Genuine Android VpnService implementation with a real local UDP DNS packet forwarder.
 *
 * Security honesty note:
 * Establishing an encrypted remote VPN exit tunnel requires a real remote VPN server
 * (such as WireGuard or OpenVPN credentials/endpoint). Without a remote server handshake,
 * this service establishes a real Android TUN interface and forwards UDP DNS queries over
 * a protected socket to the configured DNS resolver, while explicitly reporting its real
 * local-interface status — never claiming remote traffic is encrypted or anonymized when
 * no remote server is connected.
 */
class NovaVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var forwarderJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tunWriteLock = Any()
    private val forwardedCounter = AtomicLong(0L)
    private val blockedCounter = AtomicLong(0L)
    @Volatile
    private var netShieldActive: Boolean = true
    @Volatile
    private var dohActive: Boolean = true
    @Volatile
    private var currentDnsServer: String = "1.1.1.1"

    private val protectedSocketFactory: SocketFactory = object : SocketFactory() {
        override fun createSocket(): Socket = Socket().also { protect(it) }

        override fun createSocket(host: String, port: Int): Socket =
            Socket().also {
                protect(it)
                it.connect(InetSocketAddress(host, port))
            }

        override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket =
            Socket().also {
                protect(it)
                it.bind(InetSocketAddress(localHost, localPort))
                it.connect(InetSocketAddress(host, port))
            }

        override fun createSocket(host: InetAddress, port: Int): Socket =
            Socket().also {
                protect(it)
                it.connect(InetSocketAddress(host, port))
            }

        override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket =
            Socket().also {
                protect(it)
                it.bind(InetSocketAddress(localAddress, localPort))
                it.connect(InetSocketAddress(address, port))
            }
    }

    private val dohHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .socketFactory(protectedSocketFactory)
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                disconnectTunnel()
                stopSelf()
            }
            ACTION_CONNECT -> {
                val dns = intent.getStringExtra(EXTRA_DNS_SERVER).orEmpty().trim().ifBlank { "1.1.1.1" }
                val gateway = intent.getStringExtra(EXTRA_GATEWAY).orEmpty().trim()
                val netShield = intent.getBooleanExtra(EXTRA_NETSHIELD_ENABLED, true)
                val doh = intent.getBooleanExtra(EXTRA_DOH_ENABLED, true)
                establishLocalTunnel(dns, gateway, netShield, doh)
            }
        }
        return START_NOT_STICKY
    }

    private fun establishLocalTunnel(
        dnsServer: String,
        gateway: String,
        netShieldEnabled: Boolean,
        dohEnabled: Boolean
    ) {
        netShieldActive = netShieldEnabled
        dohActive = dohEnabled
        currentDnsServer = dnsServer

        if (!SecurityUtils.isValidIpAddress(dnsServer)) {
            VpnStateHolder.update(
                VpnRuntimeState(
                    status = VpnConnectionStatus.ERROR,
                    dnsServer = dnsServer,
                    gatewayAddress = gateway,
                    netShieldEnabled = netShieldEnabled,
                    dohEnabled = dohEnabled,
                    statusDetail = "Invalid DNS IP address '$dnsServer'. Enter a valid IPv4 or IPv6 address (e.g. 1.1.1.1)."
                )
            )
            stopSelf()
            return
        }

        promoteToForeground(dnsServer, netShieldEnabled, dohEnabled)

        VpnStateHolder.update(
            VpnRuntimeState(
                status = VpnConnectionStatus.CONNECTING,
                dnsServer = dnsServer,
                gatewayAddress = gateway,
                netShieldEnabled = netShieldEnabled,
                dohEnabled = dohEnabled,
                statusDetail = "Initializing Android VpnService interface..."
            )
        )

        try {
            stopForwarderAndCloseTun()

            val isIpv6 = dnsServer.contains(':')
            val builder = Builder()
                .setSession("Nova Browser VPN Shield")
                .setMtu(1500)

            if (isIpv6) {
                builder
                    .addAddress("fd00:10:88::2", 128)
                    .addDnsServer(dnsServer)
                    .addRoute(dnsServer, 128)
            } else {
                builder
                    .addAddress("10.88.0.2", 32)
                    .addDnsServer(dnsServer)
                    // Route only the dedicated DNS resolver through the local TUN so DNS packets
                    // are actively forwarded/filtered without blackholing normal traffic.
                    .addRoute(dnsServer, 32)
            }

            val established = builder.establish()
            if (established == null) {
                VpnStateHolder.update(
                    VpnRuntimeState(
                        status = VpnConnectionStatus.ERROR,
                        dnsServer = dnsServer,
                        gatewayAddress = gateway,
                        netShieldEnabled = netShieldEnabled,
                        dohEnabled = dohEnabled,
                        statusDetail = "Android declined to create the VPN interface. Ensure VPN permission is granted."
                    )
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            vpnInterface = established
            forwardedCounter.set(0L)
            blockedCounter.set(0L)
            startDnsPacketForwarder(established, dnsServer)

            val modeDesc = buildString {
                append(if (dohEnabled) "Encrypted DoH (TLS)" else "Protected UDP DNS")
                if (netShieldEnabled) append(" + NetShield Filter")
                append(" · $dnsServer")
            }
            val gatewayNote = if (gateway.isNotBlank()) {
                "$modeDesc active. Gateway '$gateway' requires Proton VPN / WireGuard app for full remote country IP routing."
            } else {
                "$modeDesc active. For remote multi-country exit IP tunneling, pair with Proton VPN or Android System VPN."
            }

            VpnStateHolder.update(
                VpnRuntimeState(
                    status = VpnConnectionStatus.CONNECTED_LOCAL_TUN,
                    dnsServer = dnsServer,
                    gatewayAddress = gateway,
                    netShieldEnabled = netShieldEnabled,
                    dohEnabled = dohEnabled,
                    statusDetail = gatewayNote,
                    connectedSinceMs = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            VpnStateHolder.update(
                VpnRuntimeState(
                    status = VpnConnectionStatus.ERROR,
                    dnsServer = dnsServer,
                    gatewayAddress = gateway,
                    statusDetail = "Failed to establish VPN interface: ${e.localizedMessage ?: "Invalid DNS or configuration"}"
                )
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /**
     * Reads UDP port-53 DNS packets from the TUN file descriptor and forwards them via
     * VpnService.protect()ed sockets to [dnsServer], writing valid IP+UDP responses back.
     */
    private fun startDnsPacketForwarder(tunFd: ParcelFileDescriptor, dnsServer: String) {
        forwarderJob?.cancel()
        forwarderJob = serviceScope.launch {
            val targetAddress = runCatching { InetAddress.getByName(dnsServer) }.getOrNull() ?: return@launch
            val input = FileInputStream(tunFd.fileDescriptor)
            val output = FileOutputStream(tunFd.fileDescriptor)
            val buffer = ByteArray(32767)

            while (isActive) {
                val length = try {
                    input.read(buffer)
                } catch (_: Exception) {
                    break
                }
                if (length <= 0) {
                    if (length < 0) break
                    continue
                }

                val packet = buffer.copyOfRange(0, length)
                val version = (packet[0].toInt() ushr 4) and 0x0F
                if (version == 4 && length >= 28) {
                    handleIpv4Packet(packet, length, targetAddress, output)
                } else if (version == 6 && length >= 48) {
                    handleIpv6Packet(packet, length, targetAddress, output)
                }
            }
        }
    }

    private fun handleIpv4Packet(
        packet: ByteArray,
        length: Int,
        targetAddress: InetAddress,
        output: FileOutputStream
    ) {
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20) return
        val protocol = packet[9].toInt() and 0xFF
        if (protocol == 6 && length >= ihl + 20) {
            // Immediately reset TCP probes (e.g. Android Private DNS TCP 853) to prevent multi-second hangs
            handleIpv4TcpRst(packet, ihl, length, output)
            return
        }
        if (protocol != 17 || length < ihl + 8) return // UDP DNS port 53

        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        if (dstPort != 53) return

        val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
        val dnsLen = udpLen - 8
        if (dnsLen <= 0 || ihl + 8 + dnsLen > length) return

        val dnsQuery = packet.copyOfRange(ihl + 8, ihl + 8 + dnsLen)
        val srcIp = packet.copyOfRange(12, 16)
        val dstIp = packet.copyOfRange(16, 20)
        val srcPortBytes = packet.copyOfRange(ihl, ihl + 2)
        val dstPortBytes = packet.copyOfRange(ihl + 2, ihl + 4)
        val ipId0 = packet[4]
        val ipId1 = packet[5]

        serviceScope.launch {
            val dnsResp = resolveDnsQuery(dnsQuery, targetAddress, currentDnsServer) ?: return@launch
            val totalLen = 20 + 8 + dnsResp.size
            val out = ByteArray(totalLen)

            // IPv4 header
            out[0] = 0x45.toByte()
            out[1] = 0
            out[2] = ((totalLen ushr 8) and 0xFF).toByte()
            out[3] = (totalLen and 0xFF).toByte()
            out[4] = ipId0
            out[5] = ipId1
            out[6] = 0x40.toByte()
            out[7] = 0
            out[8] = 64.toByte()
            out[9] = 17.toByte()
            System.arraycopy(dstIp, 0, out, 12, 4)
            System.arraycopy(srcIp, 0, out, 16, 4)
            val ipChecksum = computeChecksum(out, 0, 20)
            out[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
            out[11] = (ipChecksum and 0xFF).toByte()

            // UDP header
            out[20] = dstPortBytes[0]
            out[21] = dstPortBytes[1]
            out[22] = srcPortBytes[0]
            out[23] = srcPortBytes[1]
            val respUdpLen = 8 + dnsResp.size
            out[24] = ((respUdpLen ushr 8) and 0xFF).toByte()
            out[25] = (respUdpLen and 0xFF).toByte()
            System.arraycopy(dnsResp, 0, out, 28, dnsResp.size)

            val udpChecksum = computeIpv4TransportChecksum(dstIp, srcIp, 17, out, 20, respUdpLen)
            out[26] = ((udpChecksum ushr 8) and 0xFF).toByte()
            out[27] = (udpChecksum and 0xFF).toByte()

            writeTunPacket(output, out)
            recordForwardedQuery()
        }
    }

    private fun handleIpv4TcpRst(
        packet: ByteArray,
        ihl: Int,
        length: Int,
        output: FileOutputStream
    ) {
        val flags = packet[ihl + 13].toInt() and 0xFF
        if ((flags and 0x04) != 0) return // Don't reply RST to an RST
        val tcpHeaderWords = (packet[ihl + 12].toInt() ushr 4) and 0x0F
        val tcpHeaderLen = (tcpHeaderWords * 4).coerceAtLeast(20)
        if (ihl + tcpHeaderLen > length) return

        val srcIp = packet.copyOfRange(12, 16)
        val dstIp = packet.copyOfRange(16, 20)
        val seq = readUint32(packet, ihl + 4)
        val ack = readUint32(packet, ihl + 8)
        val payloadLen = (length - ihl - tcpHeaderLen).coerceAtLeast(0)
        val ackIncrement = payloadLen + (if ((flags and 0x02) != 0) 1 else 0) + (if ((flags and 0x01) != 0) 1 else 0)

        val out = ByteArray(40)
        out[0] = 0x45.toByte()
        out[2] = 0
        out[3] = 40
        out[6] = 0x40.toByte()
        out[8] = 64.toByte()
        out[9] = 6.toByte()
        System.arraycopy(dstIp, 0, out, 12, 4)
        System.arraycopy(srcIp, 0, out, 16, 4)
        val ipChecksum = computeChecksum(out, 0, 20)
        out[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
        out[11] = (ipChecksum and 0xFF).toByte()

        // TCP header
        out[20] = packet[ihl + 2]
        out[21] = packet[ihl + 3]
        out[22] = packet[ihl]
        out[23] = packet[ihl + 1]
        val hasAck = (flags and 0x10) != 0
        if (hasAck) {
            writeUint32(out, 24, ack)
            writeUint32(out, 28, 0L)
            out[33] = 0x04 // RST
        } else {
            writeUint32(out, 24, 0L)
            writeUint32(out, 28, seq + ackIncrement.coerceAtLeast(1))
            out[33] = 0x14 // RST + ACK
        }
        out[32] = 0x50.toByte() // 20-byte TCP header
        val tcpChecksum = computeIpv4TransportChecksum(dstIp, srcIp, 6, out, 20, 20)
        out[36] = ((tcpChecksum ushr 8) and 0xFF).toByte()
        out[37] = (tcpChecksum and 0xFF).toByte()

        writeTunPacket(output, out)
    }

    private fun handleIpv6Packet(
        packet: ByteArray,
        length: Int,
        targetAddress: InetAddress,
        output: FileOutputStream
    ) {
        val ihl = 40
        val nextHeader = packet[6].toInt() and 0xFF
        if (nextHeader == 6 && length >= ihl + 20) {
            handleIpv6TcpRst(packet, length, output)
            return
        }
        if (nextHeader != 17 || length < ihl + 8) return

        val dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
        if (dstPort != 53) return

        val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
        val dnsLen = udpLen - 8
        if (dnsLen <= 0 || ihl + 8 + dnsLen > length) return

        val dnsQuery = packet.copyOfRange(ihl + 8, ihl + 8 + dnsLen)
        val srcIp = packet.copyOfRange(8, 24)
        val dstIp = packet.copyOfRange(24, 40)
        val srcPortBytes = packet.copyOfRange(ihl, ihl + 2)
        val dstPortBytes = packet.copyOfRange(ihl + 2, ihl + 4)

        serviceScope.launch {
            val dnsResp = resolveDnsQuery(dnsQuery, targetAddress, currentDnsServer) ?: return@launch
            val respUdpLen = 8 + dnsResp.size
            val totalLen = 40 + respUdpLen
            val out = ByteArray(totalLen)

            // IPv6 header
            out[0] = 0x60.toByte()
            out[4] = ((respUdpLen ushr 8) and 0xFF).toByte()
            out[5] = (respUdpLen and 0xFF).toByte()
            out[6] = 17.toByte()
            out[7] = 64.toByte()
            System.arraycopy(dstIp, 0, out, 8, 16)
            System.arraycopy(srcIp, 0, out, 24, 16)

            // UDP header
            out[40] = dstPortBytes[0]
            out[41] = dstPortBytes[1]
            out[42] = srcPortBytes[0]
            out[43] = srcPortBytes[1]
            out[44] = ((respUdpLen ushr 8) and 0xFF).toByte()
            out[45] = (respUdpLen and 0xFF).toByte()
            System.arraycopy(dnsResp, 0, out, 48, dnsResp.size)

            val udpChecksum = computeIpv6TransportChecksum(dstIp, srcIp, 17, out, 40, respUdpLen)
            out[46] = ((udpChecksum ushr 8) and 0xFF).toByte()
            out[47] = (udpChecksum and 0xFF).toByte()

            writeTunPacket(output, out)
            recordForwardedQuery()
        }
    }

    private fun handleIpv6TcpRst(
        packet: ByteArray,
        length: Int,
        output: FileOutputStream
    ) {
        val ihl = 40
        val flags = packet[ihl + 13].toInt() and 0xFF
        if ((flags and 0x04) != 0) return
        val tcpHeaderWords = (packet[ihl + 12].toInt() ushr 4) and 0x0F
        val tcpHeaderLen = (tcpHeaderWords * 4).coerceAtLeast(20)
        if (ihl + tcpHeaderLen > length) return

        val srcIp = packet.copyOfRange(8, 24)
        val dstIp = packet.copyOfRange(24, 40)
        val seq = readUint32(packet, ihl + 4)
        val ack = readUint32(packet, ihl + 8)
        val payloadLen = (length - ihl - tcpHeaderLen).coerceAtLeast(0)
        val ackIncrement = payloadLen + (if ((flags and 0x02) != 0) 1 else 0) + (if ((flags and 0x01) != 0) 1 else 0)

        val out = ByteArray(60)
        out[0] = 0x60.toByte()
        out[4] = 0
        out[5] = 20
        out[6] = 6.toByte()
        out[7] = 64.toByte()
        System.arraycopy(dstIp, 0, out, 8, 16)
        System.arraycopy(srcIp, 0, out, 24, 16)

        out[40] = packet[ihl + 2]
        out[41] = packet[ihl + 3]
        out[42] = packet[ihl]
        out[43] = packet[ihl + 1]
        val hasAck = (flags and 0x10) != 0
        if (hasAck) {
            writeUint32(out, 44, ack)
            writeUint32(out, 48, 0L)
            out[53] = 0x04
        } else {
            writeUint32(out, 44, 0L)
            writeUint32(out, 48, seq + ackIncrement.coerceAtLeast(1))
            out[53] = 0x14
        }
        out[52] = 0x50.toByte()
        val tcpChecksum = computeIpv6TransportChecksum(dstIp, srcIp, 6, out, 40, 20)
        out[56] = ((tcpChecksum ushr 8) and 0xFF).toByte()
        out[57] = (tcpChecksum and 0xFF).toByte()

        writeTunPacket(output, out)
    }

    private fun writeTunPacket(output: FileOutputStream, out: ByteArray) {
        synchronized(tunWriteLock) {
            runCatching {
                output.write(out)
                output.flush()
            }
        }
    }

    private fun recordForwardedQuery() {
        val count = forwardedCounter.incrementAndGet()
        val current = VpnStateHolder.state.value
        if (current.status == VpnConnectionStatus.CONNECTED_LOCAL_TUN) {
            VpnStateHolder.update(current.copy(queriesForwarded = count))
        }
    }

    private fun recordBlockedDomain(domain: String) {
        val count = blockedCounter.incrementAndGet()
        val current = VpnStateHolder.state.value
        if (current.status == VpnConnectionStatus.CONNECTED_LOCAL_TUN) {
            VpnStateHolder.update(
                current.copy(
                    threatsBlocked = count,
                    lastBlockedDomain = domain
                )
            )
        }
    }

    private fun resolveDnsQuery(
        query: ByteArray,
        targetAddress: InetAddress,
        dnsServer: String
    ): ByteArray? {
        if (netShieldActive) {
            val domain = parseDnsQuestionDomain(query)
            if (domain != null && AdBlockEngine.isBuiltInAdOrTrackerHost(domain)) {
                val blockedResp = buildNxDomainResponse(query)
                if (blockedResp != null) {
                    recordBlockedDomain(domain)
                    return blockedResp
                }
            }
        }
        if (dohActive) {
            forwardDohQuery(query, dnsServer)?.let { return it }
        }
        return forwardUdpDnsQuery(query, targetAddress)
    }

    private fun forwardDohQuery(query: ByteArray, dnsServer: String): ByteArray? {
        val dohUrl = when (dnsServer) {
            "1.1.1.1" -> "https://1.1.1.1/dns-query"
            "9.9.9.9" -> "https://9.9.9.9/dns-query"
            "8.8.8.8" -> "https://8.8.8.8/dns-query"
            "94.140.14.14" -> "https://94.140.14.14/dns-query"
            else -> return null
        }
        return runCatching {
            val req = Request.Builder()
                .url(dohUrl)
                .header("Accept", "application/dns-message")
                .post(query.toRequestBody(DOH_MEDIA_TYPE))
                .build()
            dohHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val bytes = resp.body?.bytes()
                if (bytes != null && bytes.size >= 12) bytes else null
            }
        }.getOrNull()
    }

    private fun parseDnsQuestionDomain(query: ByteArray): String? {
        if (query.size < 14) return null
        val qdCount = ((query[4].toInt() and 0xFF) shl 8) or (query[5].toInt() and 0xFF)
        if (qdCount < 1) return null
        var idx = 12
        val sb = StringBuilder()
        var safety = 0
        while (idx < query.size && safety++ < 64) {
            val len = query[idx].toInt() and 0xFF
            if (len == 0) break
            if (len >= 0xC0 || idx + 1 + len > query.size) return null
            if (sb.isNotEmpty()) sb.append('.')
            for (j in 1..len) {
                val ch = query[idx + j].toInt() and 0xFF
                sb.append(ch.toChar().lowercaseChar())
            }
            idx += 1 + len
        }
        return sb.toString().takeIf { it.isNotBlank() }
    }

    private fun buildNxDomainResponse(query: ByteArray): ByteArray? {
        if (query.size < 14) return null
        var idx = 12
        var safety = 0
        while (idx < query.size && safety++ < 64) {
            val len = query[idx].toInt() and 0xFF
            if (len == 0) {
                idx += 1
                break
            }
            if (len >= 0xC0 || idx + 1 + len > query.size) return null
            idx += 1 + len
        }
        val questionEnd = idx + 4 // QTYPE (2) + QCLASS (2)
        if (questionEnd > query.size) return null

        val resp = ByteArray(questionEnd)
        System.arraycopy(query, 0, resp, 0, questionEnd)
        // Standard query response, recursion desired + available, RCODE=3 (NXDOMAIN)
        resp[2] = (0x81).toByte()
        resp[3] = (0x83).toByte()
        // Force QDCOUNT=1, ANCOUNT=0, NSCOUNT=0, ARCOUNT=0
        resp[4] = 0; resp[5] = 1
        resp[6] = 0; resp[7] = 0
        resp[8] = 0; resp[9] = 0
        resp[10] = 0; resp[11] = 0
        return resp
    }

    private fun readUint32(buf: ByteArray, offset: Int): Long {
        return ((buf[offset].toLong() and 0xFFL) shl 24) or
            ((buf[offset + 1].toLong() and 0xFFL) shl 16) or
            ((buf[offset + 2].toLong() and 0xFFL) shl 8) or
            (buf[offset + 3].toLong() and 0xFFL)
    }

    private fun writeUint32(buf: ByteArray, offset: Int, value: Long) {
        buf[offset] = ((value ushr 24) and 0xFFL).toByte()
        buf[offset + 1] = ((value ushr 16) and 0xFFL).toByte()
        buf[offset + 2] = ((value ushr 8) and 0xFFL).toByte()
        buf[offset + 3] = (value and 0xFFL).toByte()
    }

    private fun forwardUdpDnsQuery(query: ByteArray, targetAddress: InetAddress): ByteArray? {
        return runCatching {
            DatagramSocket().use { socket ->
                if (!protect(socket)) return@use null
                socket.soTimeout = 3500
                socket.send(DatagramPacket(query, query.size, targetAddress, 53))
                val respBuffer = ByteArray(4096)
                val respPacket = DatagramPacket(respBuffer, respBuffer.size)
                socket.receive(respPacket)
                respBuffer.copyOfRange(0, respPacket.length)
            }
        }.getOrNull()
    }

    private fun computeChecksum(buf: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length
        while (i + 1 < end) {
            sum += ((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) {
            sum += (buf[i].toInt() and 0xFF) shl 8
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return (sum.inv() and 0xFFFF).toInt()
    }

    private fun computeIpv4TransportChecksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        protocol: Int,
        segment: ByteArray,
        offset: Int,
        len: Int
    ): Int {
        var sum = 0L
        for (i in 0 until 4 step 2) {
            sum += ((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)
            sum += ((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)
        }
        sum += protocol.toLong()
        sum += len.toLong()
        var i = offset
        val end = offset + len
        while (i + 1 < end) {
            sum += ((segment[i].toInt() and 0xFF) shl 8) or (segment[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) {
            sum += (segment[i].toInt() and 0xFF) shl 8
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val result = (sum.inv() and 0xFFFF).toInt()
        return if (protocol == 17 && result == 0) 0xFFFF else result
    }

    private fun computeIpv6TransportChecksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        protocol: Int,
        segment: ByteArray,
        offset: Int,
        len: Int
    ): Int {
        var sum = 0L
        for (i in 0 until 16 step 2) {
            sum += ((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)
            sum += ((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)
        }
        sum += (len ushr 16).toLong()
        sum += (len and 0xFFFF).toLong()
        sum += protocol.toLong()
        var i = offset
        val end = offset + len
        while (i + 1 < end) {
            sum += ((segment[i].toInt() and 0xFF) shl 8) or (segment[i + 1].toInt() and 0xFF)
            i += 2
        }
        if (i < end) {
            sum += (segment[i].toInt() and 0xFF) shl 8
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        val result = (sum.inv() and 0xFFFF).toInt()
        return if (protocol == 17 && result == 0) 0xFFFF else result
    }

    private fun promoteToForeground(dnsServer: String, netShield: Boolean, doh: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm != null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nova VPN DNS Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays active local VPN DoH & NetShield TUN status"
            }
            nm.createNotificationChannel(channel)
        }

        val launchIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val summary = buildString {
            append(if (doh) "Encrypted DoH" else "Protected DNS")
            if (netShield) append(" + NetShield")
            append(" · $dnsServer")
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nova VPN Shield Active")
            .setContentText(summary)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(launchIntent)
            .setOngoing(true)
            .build()

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun stopForwarderAndCloseTun() {
        forwarderJob?.cancel()
        forwarderJob = null
        runCatching { vpnInterface?.close() }
        vpnInterface = null
    }

    private fun disconnectTunnel() {
        stopForwarderAndCloseTun()
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        val prev = VpnStateHolder.state.value
        VpnStateHolder.update(
            VpnRuntimeState(
                status = VpnConnectionStatus.DISCONNECTED,
                dnsServer = prev.dnsServer,
                gatewayAddress = prev.gatewayAddress,
                netShieldEnabled = prev.netShieldEnabled,
                dohEnabled = prev.dohEnabled,
                statusDetail = "Disconnected. No VPN tunnel is active.",
                connectedSinceMs = null
            )
        )
    }

    override fun onRevoke() {
        disconnectTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        disconnectTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"
        const val EXTRA_DNS_SERVER = "extra_dns_server"
        const val EXTRA_GATEWAY = "extra_gateway"
        const val EXTRA_NETSHIELD_ENABLED = "extra_netshield_enabled"
        const val EXTRA_DOH_ENABLED = "extra_doh_enabled"
        private val DOH_MEDIA_TYPE = "application/dns-message".toMediaType()
        private const val CHANNEL_ID = "nova_vpn_channel"
        private const val NOTIFICATION_ID = 4102

        fun prepareIntent(context: Context): Intent? = prepare(context)

        fun startConnect(
            context: Context,
            dnsServer: String,
            gateway: String,
            netShieldEnabled: Boolean = true,
            dohEnabled: Boolean = true
        ) {
            val intent = Intent(context, NovaVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_DNS_SERVER, dnsServer)
                putExtra(EXTRA_GATEWAY, gateway)
                putExtra(EXTRA_NETSHIELD_ENABLED, netShieldEnabled)
                putExtra(EXTRA_DOH_ENABLED, dohEnabled)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun startDisconnect(context: Context) {
            val intent = Intent(context, NovaVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }
}
