package com.client.xvideos.common.p2p

import android.content.Context
import android.os.Build
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.p2p.imports.StoreBundleImporter
import com.client.xvideos.common.p2p.nearby.NearbyClientImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * Менеджер для управления P2P приемом через GlobalScope.
 * Работает только пока приложение активно (не в фоне).
 */
object P2pReceiveManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _controller = MutableStateFlow<P2pReceiveController?>(null)
    val controller: StateFlow<P2pReceiveController?> = _controller.asStateFlow()
    
    private var job: Job? = null

    fun start(context: Context) {
        if (_controller.value != null) {
            // Контроллер уже существует (например, после ошибки или чужого stopAll):
            // повторный вход на экран должен оживить рекламу, а не стать no-op.
            ensureAdvertising()
            return
        }

        Timber.d("P2P Manager: Starting")
        val nearby = NearbyClientImpl(context.applicationContext)

        val importer = StoreBundleImporter(
            storeRootFor = { type ->
                when (type) {
                    P2pType.X -> File(AppPath.x_cache_download)
                    P2pType.R -> File(AppPath.r_cache_download)
                    P2pType.L -> File(AppPath.l_likes)
                }
            },
            refreshFor = { /* ... */ }
        )

        val newController = P2pReceiveController(
            nearby = nearby,
            importer = importer,
            scope = scope,
            deviceName = Build.MODEL ?: "Android"
        )
        _controller.value = newController

        job = scope.launch {
            newController.state.collectLatest { state ->
                handleStateChange(state)
            }
        }

        newController.start()
    }

    fun stop() {
        Timber.d("P2P Manager: Stopping")
        _controller.value?.stop()
        _controller.value = null
        job?.cancel()
        job = null
    }

    /**
     * Перезапускает рекламу, если контроллер существует и не занят активной передачей.
     * Нужен после чужого stopAll (экран отправки гасит рекламу всего процесса)
     * и при повторном входе на экран приёма.
     */
    fun ensureAdvertising() {
        val controller = _controller.value ?: return
        when (controller.state.value) {
            is ReceiveState.Connecting, is ReceiveState.Receiving -> return
            else -> {
                Timber.d("P2P Manager: ensureAdvertising → restarting advertising")
                controller.start()
            }
        }
    }

    private suspend fun handleStateChange(state: ReceiveState) {
        when (state) {
            is ReceiveState.Receiving -> {
                EventBus.postEvent(Event.P2pTransferUpdate.Progress("Устройство", state.transferred, state.total))
            }
            is ReceiveState.Done -> {
                EventBus.postEvent(Event.P2pTransferUpdate.Success("Устройство"))
                _controller.value?.start()
            }
            is ReceiveState.Error -> {
                EventBus.postEvent(Event.P2pTransferUpdate.Error("Устройство", state.message))
                Timber.w("P2P Manager: Error state: ${state.message}. Restarting advertising in 5s.")
                // Пауза защищает от горячего цикла ошибок; collectLatest отменит
                // перезапуск, если состояние успеет смениться.
                kotlinx.coroutines.delay(5_000)
                _controller.value?.start()
            }
            else -> {}
        }
    }
}

fun toggleP2pService(context: Context, enabled: Boolean) {
    if (enabled) {
        P2pReceiveManager.start(context)
    } else {
        P2pReceiveManager.stop()
    }
}
