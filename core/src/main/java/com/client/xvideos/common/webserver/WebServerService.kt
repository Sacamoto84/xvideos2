package com.client.xvideos.common.webserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.core.R
import timber.log.Timber

class WebServerService : Service() {

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> {
                val port = intent?.getIntExtra(EXTRA_PORT, Settings.web_server_port.field.value) ?: 8080
                startWebServer(port)
            }
            ACTION_STOP -> {
                stopWebServer()
            }
        }
        return START_NOT_STICKY
    }

    private fun startWebServer(port: Int) {
        val appContext = applicationContext
        val ip = NetworkIpHelper.getLocalIpAddress(appContext) ?: "127.0.0.1"
        val serverUrl = NetworkIpHelper.buildServerUrl(ip, port)

        val notification = createNotification(serverUrl)
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }

        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
        } catch (e: Exception) {
            Timber.e(e, "WebServerService: не удалось запустить ForegroundService")
        }

        acquireLocks()

        val result = LocalWebServer.start(appContext, port)
        if (result.isFailure) {
            Timber.e("WebServerService: ошибка старта Ktor сервера")
            stopSelf()
        }
    }

    private fun stopWebServer() {
        LocalWebServer.stop()
        releaseLocks()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireLocks() {
        val keepAwake = Settings.web_server_keep_awake.field.value
        if (!keepAwake) return

        runCatching {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiLock == null) {
                @Suppress("DEPRECATION")
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                } else {
                    WifiManager.WIFI_MODE_FULL
                }
                wifiLock = wifiManager?.createWifiLock(mode, "Xvideos:WebServerWifiLock")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }

            val powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Xvideos:WebServerWakeLock")?.apply {
                    setReferenceCounted(false)
                    acquire(2 * 60 * 60 * 1000L) // 2 hours max
                }
            }
        }.onFailure {
            Timber.w(it, "WebServerService: не удалось захватить WakeLock/WifiLock")
        }
    }

    private fun releaseLocks() {
        runCatching {
            wifiLock?.let {
                if (it.isHeld) it.release()
            }
            wifiLock = null

            wakeLock?.let {
                if (it.isHeld) it.release()
            }
            wakeLock = null
        }.onFailure {
            Timber.w(it, "WebServerService: ошибка освобождения Locks")
        }
    }

    private fun createNotification(url: String): Notification {
        val stopIntent = Intent(this, WebServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentPendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Веб-сервер активен")
            .setContentText("$url — трансляция на ПК")
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Остановить", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Локальный веб-сервер",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о работе локального HTTP-сервера для трансляции на ПК"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        LocalWebServer.stop()
        releaseLocks()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "web_server_channel"
        const val NOTIFICATION_ID = 20260917
        const val ACTION_START = "com.client.xvideos.webserver.START"
        const val ACTION_STOP = "com.client.xvideos.webserver.STOP"
        const val EXTRA_PORT = "extra_port"

        fun start(context: Context, port: Int = 8080) {
            val intent = Intent(context, WebServerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PORT, port)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WebServerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
