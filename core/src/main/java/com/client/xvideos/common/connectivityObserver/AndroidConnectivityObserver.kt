package com.client.xvideos.common.connectivityObserver

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.getSystemService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Singleton

interface ConnectivityObserver {
    val isConnected: StateFlow<Boolean>
}

@Module
@InstallIn(SingletonComponent::class)
object ConnectivityModule {

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        @ApplicationContext context: Context,
    ): ConnectivityObserver {
        return AndroidConnectivityObserver(context, CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }

}

class AndroidConnectivityObserver(
    private val context: Context,
    private val scope: CoroutineScope
) : ConnectivityObserver {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var networkCallback: NetworkCallback? = null

    init {
        updateInitialConnectionState()
        registerNetworkCallback()

        // Автоматически отменяем callback при отмене scope
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                unregisterNetworkCallback()
            }
        }
    }

    private fun updateInitialConnectionState() {
        val cm = connectivityManager
        if (cm == null) {
            _isConnected.value = false
            return
        }
        try {
            val activeNetwork = cm.activeNetwork
            val networkCapabilities = activeNetwork?.let {
                cm.getNetworkCapabilities(it)
            }

            val hasInternet = networkCapabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) == true

            val isValidated = networkCapabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            ) == true

            val isConnected = hasInternet && isValidated
            _isConnected.value = isConnected

            Timber.d("Initial connection state - hasInternet: $hasInternet, isValidated: $isValidated, connected: $isConnected")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to get initial connection state")
            _isConnected.value = false
        }
    }

    private fun registerNetworkCallback() {
        val cm = connectivityManager ?: return
        val callback = object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.d("Network onAvailable: $network")
                val caps = cm.getNetworkCapabilities(network)
                if (caps != null) {
                    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    _isConnected.value = hasInternet && isValidated
                }
            }

            override fun onLost(network: Network) {
                Timber.d("Network onLost: $network")
                val currentActive = cm.activeNetwork
                if (currentActive == null || currentActive == network) {
                    _isConnected.value = false
                } else {
                    val caps = cm.getNetworkCapabilities(currentActive)
                    val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    val isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
                    _isConnected.value = hasInternet && isValidated
                }
            }

            override fun onUnavailable() {
                Timber.d("Network onUnavailable")
                _isConnected.value = false
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val isValidated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

                val isConnected = hasInternet && isValidated

                Timber.d("Network onCapabilitiesChanged - network: $network, hasInternet: $hasInternet, isValidated: $isValidated, connected: $isConnected")
                _isConnected.value = isConnected
            }
        }

        networkCallback = callback

        try {
            Timber.d("Registering default network callback")
            cm.registerDefaultNetworkCallback(callback)
        } catch (e: Exception) {
            Timber.e(e, "Failed to register network callback")
        }
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        networkCallback = null
        try {
            connectivityManager?.unregisterNetworkCallback(callback)
            Timber.d("unregisterNetworkCallback success")
        } catch (e: Exception) {
            Timber.e(e, "Failed to unregister network callback")
        }
    }
}
