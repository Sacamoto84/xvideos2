package com.client.xvideos.common.webserver

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Реактивное состояние локального веб-сервера.
 * Наблюдается UI-экранами и сервисом.
 */
object WebServerState {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    private val _ipAddress = MutableStateFlow<String?>(null)
    val ipAddress: StateFlow<String?> = _ipAddress.asStateFlow()

    private val _port = MutableStateFlow(8080)
    val port: StateFlow<Int> = _port.asStateFlow()

    private val _networkName = MutableStateFlow("Wi-Fi")
    val networkName: StateFlow<String> = _networkName.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun updateRunning(
        running: Boolean,
        url: String? = null,
        ip: String? = null,
        port: Int = 8080,
        netName: String = "Wi-Fi"
    ) {
        _isRunning.value = running
        _serverUrl.value = url
        _ipAddress.value = ip
        _port.value = port
        _networkName.value = netName
        if (running) {
            _lastError.value = null
        }
    }

    fun clearError() {
        _lastError.value = null
    }

    fun setError(error: String?) {
        _lastError.value = error
    }
}
