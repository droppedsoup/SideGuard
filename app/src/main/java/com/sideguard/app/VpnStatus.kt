package com.sideguard.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Lightweight read-only view of the VPN situation. Does not touch Mullvad. */
object VpnStatus {

    private const val MULLVAD_PKG = "net.mullvad.mullvadvpn"

    data class State(
        val vpnActive: Boolean,
        val mullvadInstalled: Boolean
    )

    fun read(context: Context): State {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var vpn = false
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                vpn = true
                break
            }
        }
        val mullvad = try {
            context.packageManager.getPackageInfo(MULLVAD_PKG, 0)
            true
        } catch (e: Exception) {
            false
        }
        return State(vpnActive = vpn, mullvadInstalled = mullvad)
    }

    fun openMullvad(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(MULLVAD_PKG)
        if (intent != null) context.startActivity(intent)
    }
}
