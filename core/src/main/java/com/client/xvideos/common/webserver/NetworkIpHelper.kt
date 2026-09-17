package com.client.xvideos.common.webserver

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import timber.log.Timber
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkIpHelper {

    /**
     * Возвращает локальный IPv4-адрес устройства в текущей сети Wi-Fi или в режиме точки доступа.
     * Приоритет:
     * 1. Активное сетевое соединение через ConnectivityManager (Android 10+).
     * 2. Перебор сетевых интерфейсов (wlan, ap, rndis, eth) для поддержки Hotspot / USB-тетеринга.
     */
    fun getLocalIpAddress(context: Context): String? {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val linkProps = activeNetwork?.let { cm.getLinkProperties(it) }

            val ipFromActiveNetwork = linkProps?.linkAddresses
                ?.map { it.address }
                ?.filterIsInstance<Inet4Address>()
                ?.firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
                ?.hostAddress

            if (!ipFromActiveNetwork.isNullOrBlank()) {
                return@runCatching ipFromActiveNetwork
            }

            // Fallback: сканирование интерфейсов (для режима Wi-Fi Hotspot / раздачи с телефона)
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            val prioritizedInterfaces = interfaces.sortedByDescending { nif ->
                val name = nif.name.lowercase()
                when {
                    name.startsWith("wlan") -> 3
                    name.startsWith("ap") -> 2
                    name.startsWith("rndis") || name.startsWith("eth") -> 1
                    else -> 0
                }
            }

            for (nif in prioritizedInterfaces) {
                if (!nif.isUp || nif.isLoopback) continue
                for (addr in Collections.list(nif.inetAddresses)) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                        val host = addr.hostAddress
                        if (!host.isNullOrBlank()) return@runCatching host
                    }
                }
            }
            null
        }.onFailure {
            Timber.e(it, "NetworkIpHelper: ошибка определения IP адреса")
        }.getOrNull()
    }

    /**
     * Возвращает имя текущей сети Wi-Fi (SSID) или описание режима, если доступно.
     */
    @Suppress("DEPRECATION")
    fun getNetworkName(context: Context): String {
        return runCatching {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val info = wifiManager?.connectionInfo
            val ssid = info?.ssid?.removeSurrounding("\"")
            if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") {
                ssid
            } else {
                val ip = getLocalIpAddress(context)
                if (ip != null) "Локальная сеть Wi-Fi" else "Нет подключения"
            }
        }.getOrDefault("Wi-Fi")
    }

    fun buildServerUrl(ip: String, port: Int): String = "http://$ip:$port"
}
