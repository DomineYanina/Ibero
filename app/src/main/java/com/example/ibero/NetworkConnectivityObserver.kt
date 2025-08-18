package com.example.ibero

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ConnectionStatus {
    object Available : ConnectionStatus()
    object Unavailable : ConnectionStatus()
    object Losing : ConnectionStatus()
    object Lost : ConnectionStatus()
}

class NetworkConnectivityObserver(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Unavailable)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    fun observe() {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _connectionStatus.value = ConnectionStatus.Available
            }

            override fun onLost(network: Network) {
                _connectionStatus.value = ConnectionStatus.Lost
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                _connectionStatus.value = ConnectionStatus.Losing
            }

            override fun onUnavailable() {
                _connectionStatus.value = ConnectionStatus.Unavailable
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    // Método para verificar el estado actual de forma instantánea
    @RequiresApi(Build.VERSION_CODES.M)
    fun hasInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}