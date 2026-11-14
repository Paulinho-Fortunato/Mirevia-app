package ao.mirevia.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Uses OS connectivity capabilities; does not send telemetry or perform probing requests. */
class NetworkMonitor(context: Context) {
    private val manager =
        requireNotNull(context.applicationContext.getSystemService(ConnectivityManager::class.java))

    private fun connected(): Boolean {
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    val isOnline =
        callbackFlow {
                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            trySend(connected())
                        }

                        override fun onLost(network: Network) {
                            trySend(connected())
                        }

                        override fun onCapabilitiesChanged(
                            network: Network,
                            capabilities: NetworkCapabilities,
                        ) {
                            trySend(connected())
                        }
                    }
                manager.registerDefaultNetworkCallback(callback)
                trySend(connected())
                awaitClose { manager.unregisterNetworkCallback(callback) }
            }
            .distinctUntilChanged()
}
