package com.client.xvideos.common.connectivityObserver

import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
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
        //@ApplicationScope scope: CoroutineScope
    ): ConnectivityObserver {
        return AndroidConnectivityObserver(context, CoroutineScope(SupervisorJob() + Dispatchers.IO))
    }

}



class AndroidConnectivityObserver(
    private val context: Context,
    private val scope: CoroutineScope
) : ConnectivityObserver {

    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private var networkCallback: NetworkCallback? = null

    init {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            _isConnected.value = true
        }
        else
        {
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

    }

    private fun updateInitialConnectionState() {
        try {
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = activeNetwork?.let {
                connectivityManager.getNetworkCapabilities(it)
            }

            val hasInternet = networkCapabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            ) == true

            val isValidated = networkCapabilities?.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            ) == true

            val isConnected = hasInternet && isValidated
            _isConnected.value = isConnected

            Timber.w("!!! 999 Initial state - hasInternet: $hasInternet, isValidated: $isValidated, connected: $isConnected")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "!!! 999 Failed to get initial connection state")
            _isConnected.value = false
        }
    }

    private fun registerNetworkCallback() {
        val callback = object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.w("!!! 999 onAvailable: $network")
                // Не устанавливаем сразу true, ждем onCapabilitiesChanged
            }

            override fun onLost(network: Network) {
                Timber.w("!!! 999 onLost: $network")
                _isConnected.value = false
            }

            override fun onUnavailable() {
                Timber.w("!!! 999 onUnavailable")
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

                Timber.w("!!! 999 onCapabilitiesChanged - network: $network, hasInternet: $hasInternet, isValidated: $isValidated, connected: $isConnected")
                _isConnected.value = isConnected
            }
        }

        networkCallback = callback

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Timber.w("!!! 999 registerDefaultNetworkCallback")
                connectivityManager.registerDefaultNetworkCallback(callback)
            } else {
                Timber.w("!!! 999 registerNetworkCallback (legacy)")
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, callback)
            }
        } catch (e: Exception) {
            Timber.e(e, "!!! 999 Failed to register network callback")
        }
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let { callback ->
            try {
                connectivityManager.unregisterNetworkCallback(callback)
                Timber.w("!!! 999 unregisterNetworkCallback success")
            } catch (e: Exception) {
                Timber.e(e, "!!! 999 Failed to unregister network callback")
            }
            networkCallback = null
        }
    }
}

