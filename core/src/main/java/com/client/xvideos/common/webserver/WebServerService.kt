package com.client.xvideos.common.webserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
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
                val defaultPort = if (Settings.isInitialized) Settings.web_server_port.field.value else 8080
                val port = intent?.getIntExtra(EXTRA_PORT, defaultPort) ?: defaultPort
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
            // Система отказала в foreground-статусе — например, суточный предел
            // работы сервиса уже исчерпан. Раньше отказ только писался в лог, и
            // сервер запускался обычным фоновым сервисом, который система гасит.
            Timber.e(e, "WebServerService: не удалось запустить ForegroundService")
            WebServerState.setError("Android не разрешил запустить веб-сервер в фоне")
            stopSelf()
            return
        }

        acquireLocks()

        val result = LocalWebServer.start(appContext, port)
        if (result.isFailure) {
            Timber.e("WebServerService: ошибка старта Ktor сервера")
            stopSelf()
            return
        }
        watchNetwork()
    }

    private fun stopWebServer() {
        unwatchNetwork()
        LocalWebServer.stop()
        releaseLocks()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Android 15 и новее: сервису типа `dataSync` отведено шесть часов в сутки.
     * Когда они истекли, система зовёт этот метод и ждёт остановки несколько
     * секунд; сервис, который не остановился, она завершает исключением — вместе
     * с приложением.
     */
    override fun onTimeout(startId: Int, fgsType: Int) {
        Timber.w("WebServerService: исчерпан суточный предел работы сервиса, останавливаем сервер")
        stopWebServer()
        WebServerState.setError("Android остановил веб-сервер: исчерпан суточный предел работы в фоне")
    }

    /**
     * Сеть сменилась — у телефона другой адрес. Адрес и QR в настройках и текст
     * уведомления считались один раз при запуске и оставались прежними, пока
     * сервер не перезапускали.
     */
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = refreshAddress()
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) = refreshAddress()
        override fun onLost(network: Network) = refreshAddress()
    }
    private var watchingNetwork = false

    private fun watchNetwork() {
        if (watchingNetwork) return
        runCatching {
            val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            manager?.registerDefaultNetworkCallback(networkCallback)
            watchingNetwork = manager != null
        }.onFailure { Timber.w(it, "WebServerService: не удалось подписаться на смену сети") }
    }

    private fun unwatchNetwork() {
        if (!watchingNetwork) return
        watchingNetwork = false
        runCatching {
            (getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.unregisterNetworkCallback(networkCallback)
        }
    }

    private fun refreshAddress() {
        val url = LocalWebServer.refreshAddress(applicationContext) ?: return
        runCatching {
            (getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                ?.notify(NOTIFICATION_ID, createNotification(url))
        }
    }

    private fun acquireLocks() {
        val keepAwake = if (Settings.isInitialized) Settings.web_server_keep_awake.field.value else true
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
                    // Без срока: живёт, пока работает сервер, и отпускается вместе с
                    // ним. Раньше замок отпускался сам через два часа, а сервер и
                    // уведомление оставались — с погашенным экраном он молчал.
                    acquire()
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

        val isCamouflage = Settings.isInitialized && Settings.camouflage_calculator_enabled.field.value
        val title = if (isCamouflage) "Служба передачи данных" else "Веб-сервер активен"
        val text = if (isCamouflage) url else "$url — трансляция на ПК"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_web_server)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Остановить", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun ensureNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val isCamouflage = Settings.isInitialized && Settings.camouflage_calculator_enabled.field.value
            val channelName = if (isCamouflage) "Служба передачи данных" else "Локальный веб-сервер"
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомления о работе локального HTTP-сервера для трансляции на ПК"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        unwatchNetwork()
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
