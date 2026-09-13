package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ConnectionStatus {
  ONLINE,
  RECONNECTING,
  OFFLINE
}

/**
 * Monitors device internet connectivity and coordinates reconnection.
 */
class NetworkConnectivityMonitor(context: Context) {

  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  private val _status = MutableStateFlow(checkInitialConnectivity())
  val status: StateFlow<ConnectionStatus> = _status.asStateFlow()

  val isOnline: Boolean
    get() = _status.value == ConnectionStatus.ONLINE

  private var onReconnectCallback: (() -> Unit)? = null

  init {
    registerNetworkCallback()
  }

  fun setOnReconnectListener(listener: () -> Unit) {
    onReconnectCallback = listener
  }

  private fun checkInitialConnectivity(): ConnectionStatus {
    val cm = connectivityManager ?: return ConnectionStatus.ONLINE
    val activeNetwork = cm.activeNetwork ?: return ConnectionStatus.OFFLINE
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return ConnectionStatus.OFFLINE
    return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    ) {
      ConnectionStatus.ONLINE
    } else {
      ConnectionStatus.OFFLINE
    }
  }

  private fun registerNetworkCallback() {
    val cm = connectivityManager ?: return
    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    try {
      cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
          scope.launch {
            val wasOffline = _status.value != ConnectionStatus.ONLINE
            _status.value = ConnectionStatus.ONLINE
            if (wasOffline) {
              onReconnectCallback?.invoke()
            }
          }
        }

        override fun onLost(network: Network) {
          scope.launch {
            _status.value = ConnectionStatus.OFFLINE
          }
        }

        override fun onLosing(network: Network, maxMsToLive: Int) {
          scope.launch {
            _status.value = ConnectionStatus.RECONNECTING
          }
        }

        override fun onUnavailable() {
          scope.launch {
            _status.value = ConnectionStatus.OFFLINE
          }
        }
      })
    } catch (_: Exception) {
      // Fallback: stay ONLINE for tests/restricted environments
      _status.value = ConnectionStatus.ONLINE
    }
  }

  fun markReconnecting() {
    _status.value = ConnectionStatus.RECONNECTING
  }

  fun markOnline() {
    _status.value = ConnectionStatus.ONLINE
  }

  fun markOffline() {
    _status.value = ConnectionStatus.OFFLINE
  }
}
