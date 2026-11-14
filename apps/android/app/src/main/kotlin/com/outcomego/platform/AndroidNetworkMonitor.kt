package com.outcomego.platform

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

/**
 * Reports the default network used by the provider. The callback is deliberately
 * small: transport teardown and reconnect remain owned by the provider service.
 */
class AndroidNetworkMonitor(
    context: Context,
    private val onAvailable: () -> Unit,
    private val onUnavailable: () -> Unit,
) {
    private val connectivity =
        context.getSystemService(ConnectivityManager::class.java)
    private var registered = false
    private var currentNetwork: Network? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (!hasInternet(network)) return
            if (currentNetwork != null && currentNetwork != network) onUnavailable()
            currentNetwork = network
            onAvailable()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            if (hasInternet(network)) {
                currentNetwork = network
                onAvailable()
            } else if (currentNetwork == network) {
                currentNetwork = null
                onUnavailable()
            }
        }

        override fun onLost(network: Network) {
            if (currentNetwork == network) currentNetwork = null
            if (!hasValidatedDefaultNetwork()) onUnavailable()
        }
    }

    fun start() {
        if (registered) return
        runCatching {
            connectivity.registerDefaultNetworkCallback(callback)
            registered = true
        }.onFailure {
            onUnavailable()
        }
    }

    fun stop() {
        if (!registered) return
        runCatching { connectivity.unregisterNetworkCallback(callback) }
        registered = false
        currentNetwork = null
    }

    private fun hasInternet(network: Network): Boolean =
        connectivity.getNetworkCapabilities(network)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

    private fun hasValidatedDefaultNetwork(): Boolean =
        connectivity.activeNetwork?.let(::hasInternet) == true
}
