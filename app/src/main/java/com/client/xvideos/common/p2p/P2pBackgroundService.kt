package com.client.xvideos.common.p2p

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.client.xvideos.R
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.p2p.imports.StoreBundleImporter
import com.client.xvideos.common.p2p.nearby.NearbyClientImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class P2pBackgroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var controller: P2pReceiveController
    
    private val channelId = "p2p_receive"
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()
        Timber.d("P2P Background Service: onCreate")
        createNotificationChannel()
        startForeground(notificationId, createNotification("Ожидание отправителя…"))

        val nearby = NearbyClientImpl(this)
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

        controller = P2pReceiveController(
            nearby = nearby,
            importer = importer,
            scope = scope,
            deviceName = Build.MODEL ?: "Android"
        )

        scope.launch {
            controller.state.collectLatest { state ->
                handleStateChange(state)
            }
        }

        controller.start()
    }

    private fun handleStateChange(state: ReceiveState) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        when (state) {
            is ReceiveState.Connecting -> {
                nm.notify(notificationId, createNotification("Подключение к ${state.endpointName}…"))
            }
            is ReceiveState.Receiving -> {
                val pct = if (state.total > 0) (state.transferred * 100 / state.total).toInt() else 0
                val msg = "Приём от… $pct%"
                nm.notify(notificationId, createNotification(msg, pct))
                
                // Также шлем в EventBus для UI (плашка сверху)
                EventBus.postEvent(Event.P2pTransferUpdate.Progress("Устройство", state.transferred, state.total))
            }
            is ReceiveState.Done -> {
                EventBus.postEvent(Event.P2pTransferUpdate.Success("Устройство"))
                nm.notify(notificationId, createNotification("Принято ✓"))
                // После успешного приема возвращаемся в режим ожидания
                controller.start() 
            }
            is ReceiveState.Error -> {
                EventBus.postEvent(Event.P2pTransferUpdate.Error("Устройство", state.message))
                nm.notify(notificationId, createNotification("Ошибка: ${state.message}"))
                controller.start()
            }
            else -> {}
        }
    }

    private fun createNotification(text: String, progress: Int = -1): android.app.Notification {
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("P2P Приём")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        if (progress >= 0) {
            builder.setProgress(100, progress, false)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "P2P Transfer"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        controller.stop()
        scope.cancel()
        Timber.d("P2P Background Service: onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

fun toggleP2pService(context: Context, enabled: Boolean) {
    val intent = Intent(context, P2pBackgroundService::class.java)
    if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    } else {
        context.stopService(intent)
    }
}
