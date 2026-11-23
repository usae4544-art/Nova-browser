package com.example.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val connectedSinceMs: Long? = null
)

object VpnStateHolder {
    private val _state = MutableStateFlow(VpnRuntimeState())
    val state: StateFlow<VpnRuntimeState> = _state.asStateFlow()

    fun update(newState: VpnRuntimeState) {
        _state.value = newState
    }
}

/**
 * Genuine Android VpnService implementation.
 *
 * Security honesty note:
 * Establishing an encrypted remote VPN exit tunnel requires a real remote VPN server
 * (such as WireGuard or OpenVPN credentials/endpoint). Without a remote server handshake,
 * this service establishes a real Android TUN interface for local DNS configuration and
 * explicitly reports its real local-interface status — never claiming remote traffic is
 * encrypted or anonymized when no remote server is connected.
 */
class NovaVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                disconnectTunnel()
                stopSelf()
            }
            ACTION_CONNECT -> {
                val dns = intent.getStringExtra(EXTRA_DNS_SERVER).orEmpty().ifBlank { "1.1.1.1" }
                val gateway = intent.getStringExtra(EXTRA_GATEWAY).orEmpty().trim()
                establishLocalTunnel(dns, gateway)
            }
        }
        return START_NOT_STICKY
    }

    private fun establishLocalTunnel(dnsServer: String, gateway: String) {
        VpnStateHolder.update(
            VpnRuntimeState(
                status = VpnConnectionStatus.CONNECTING,
                dnsServer = dnsServer,
                gatewayAddress = gateway,
                statusDetail = "Initializing Android VpnService interface..."
            )
        )

        try {
            vpnInterface?.close()
            vpnInterface = null

            val builder = Builder()
                .setSession("Nova Browser VPN Interface")
                .setMtu(1500)
                .addAddress("10.88.0.2", 32)
                .addDnsServer(dnsServer)
                // Route only the dedicated DNS resolver through the local TUN unless a real
                // remote gateway daemon is configured, so normal internet traffic is not blackholed.
                .addRoute(dnsServer, 32)

            val established = builder.establish()
            if (established == null) {
                VpnStateHolder.update(
                    VpnRuntimeState(
                        status = VpnConnectionStatus.ERROR,
                        dnsServer = dnsServer,
                        gatewayAddress = gateway,
                        statusDetail = "Android declined to create the VPN interface. Ensure VPN permission is granted."
                    )
                )
                stopSelf()
                return
            }

            vpnInterface = established
            val gatewayNote = if (gateway.isNotBlank()) {
                "Local Android TUN active (DNS $dnsServer). Configured gateway '$gateway' requires a dedicated WireGuard/OpenVPN protocol client for remote exit encryption."
            } else {
                "Local Android VpnService TUN active (DNS $dnsServer). Remote traffic encryption requires configuring a remote VPN provider."
            }

            VpnStateHolder.update(
                VpnRuntimeState(
                    status = VpnConnectionStatus.CONNECTED_LOCAL_TUN,
                    dnsServer = dnsServer,
                    gatewayAddress = gateway,
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
            stopSelf()
        }
    }

    private fun disconnectTunnel() {
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        val prev = VpnStateHolder.state.value
        VpnStateHolder.update(
            VpnRuntimeState(
                status = VpnConnectionStatus.DISCONNECTED,
                dnsServer = prev.dnsServer,
                gatewayAddress = prev.gatewayAddress,
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
        super.onDestroy()
    }

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"
        const val EXTRA_DNS_SERVER = "extra_dns_server"
        const val EXTRA_GATEWAY = "extra_gateway"

        fun prepareIntent(context: Context): Intent? = prepare(context)

        fun startConnect(context: Context, dnsServer: String, gateway: String) {
            val intent = Intent(context, NovaVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_DNS_SERVER, dnsServer)
                putExtra(EXTRA_GATEWAY, gateway)
            }
            context.startService(intent)
        }

        fun startDisconnect(context: Context) {
            val intent = Intent(context, NovaVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }
}
