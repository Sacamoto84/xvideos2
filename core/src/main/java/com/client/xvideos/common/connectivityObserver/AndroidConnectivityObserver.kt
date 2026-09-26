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

/**
 * Интерфейс мониторинга доступности сетевого подключения.
 * Предоставляет реактивный [StateFlow] с текущим статусом валидированного интернет-соединения.
 */
interface ConnectivityObserver {
    /** Поток статуса подключения (true - интернет доступен и валидирован, false - сеть отсутствует или captive portal). */
    val isConnected: StateFlow<Boolean>
}

/**
 * Расширение для мгновенной синхронной проверки наличия подключения.
 * Введено в Batch 54 для упрощения предикатов без явного чтения `.value`.
 */
fun ConnectivityObserver.hasConnection(): Boolean = isConnected.value

/**
 * Hilt-модуль предоставления синглтона [ConnectivityObserver].
 */
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

/**
 * Android-реализация [ConnectivityObserver] на основе [ConnectivityManager] и [NetworkCallback].
 *
 * Особенности:
 * - Требует обоих флагов: [NetworkCapabilities.NET_CAPABILITY_INTERNET] и [NetworkCapabilities.NET_CAPABILITY_VALIDATED],
 *   что исключает ложные срабатывания при подключении к Wi-Fi сетям без фактического выхода в интернет.
 * - Корректно обрабатывает мобильный хэндовер (переключение Wi-Fi <-> Cellular) без ложного флаппинга в disconnected.
 * - При отмене жизненного цикла [scope] автоматически отзывает регистрацию [NetworkCallback] для предотвращения утечек памяти.
 */
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

    /**
     * Опрашивает текущее активное сетевое подключение при старте приложения до прихода первого коллбэка.
     */
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

    /**
     * Регистрирует системный [NetworkCallback] для динамического отслеживания изменений статуса сети.
     */
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

    /**
     * Безопасное снятие регистрации [NetworkCallback].
     */
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
