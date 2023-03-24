package net.pelennor.garagio.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkRequest.Builder
import androidx.core.content.getSystemService
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

class ConnectivityManagerNetworkMonitor(private val context: Context): NetworkMonitor {
    private fun ConnectivityManager?.isConnected(): Boolean {
        return this?.activeNetwork?.let(::getNetworkCapabilities)?.hasCapability(NET_CAPABILITY_INTERNET) ?: false
    }
    private fun SendChannel<Boolean>.sendConnected(connectivityManager: ConnectivityManager?) {
        trySend(connectivityManager.isConnected())
    }
    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        val callback = object: ConnectivityManager.NetworkCallback() {
            private fun update() = channel.sendConnected(connectivityManager)
            override fun onAvailable(network: Network) = update()
            override fun onLost(network: Network) = update()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) = update()
        }
        connectivityManager?.registerNetworkCallback(
            Builder().addCapability(NET_CAPABILITY_INTERNET).build(),
            callback
        )
        channel.sendConnected(connectivityManager)
        awaitClose { connectivityManager?.unregisterNetworkCallback(callback) }
    }.conflate()
}