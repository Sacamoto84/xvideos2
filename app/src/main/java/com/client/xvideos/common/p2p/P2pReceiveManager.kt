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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    private var controller: P2pReceiveController? = null
    private var job: Job? = null

    fun start(context: Context) {
        if (controller != null) return

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
        controller = newController

        job = scope.launch {
            newController.state.collectLatest { state ->
                handleStateChange(state)
            }
        }

        newController.start()
    }

    fun stop() {
        Timber.d("P2P Manager: Stopping")
        controller?.stop()
        controller = null
        job?.cancel()
        job = null
    }

    private fun handleStateChange(state: ReceiveState) {
        when (state) {
            is ReceiveState.Receiving -> {
                EventBus.postEvent(Event.P2pTransferUpdate.Progress("Устройство", state.transferred, state.total))
            }
            is ReceiveState.Done -> {
                EventBus.postEvent(Event.P2pTransferUpdate.Success("Устройство"))
                controller?.start() 
            }
            is ReceiveState.Error -> {
                EventBus.postEvent(Event.P2pTransferUpdate.Error("Устройство", state.message))
                controller?.start()
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
