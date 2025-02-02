package io.gnosis.safe.helpers

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import java.io.IOException

class ConnectivityInfoProvider(private val connectivityManager: ConnectivityManager) {

    var offline: Boolean = true

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network?) {
            super.onAvailable(network)
            offline = false
        }

        override fun onLost(network: Network?) {
            super.onLost(network)
            offline = isOffline()
        }
    }

    init {
        register()
    }

    private fun isOffline(): Boolean =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)?.run {
                    !(hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                } ?: true
            } else {
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                activeNetworkInfo == null || !activeNetworkInfo.isConnected
            }


    private fun register() {
        val builder = NetworkRequest.Builder()
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
        connectivityManager.addDefaultNetworkActiveListener { offline = isOffline() }
    }

    private fun unregister() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

class Offline : IOException()
