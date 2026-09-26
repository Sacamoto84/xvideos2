package com.client.xvideos.common.webserver

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import timber.log.Timber
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkIpHelper {

    /**
     * Возвращает локальный IPv4-адрес устройства в текущей сети Wi-Fi или в режиме точки доступа (Hotspot).
     * Приоритет:
     * 1. Активное локальное соединение (Wi-Fi или Ethernet) через ConnectivityManager.
     * 2. Перебор сетевых интерфейсов с приоритетом точек доступа (ap, softap, swlan, wlan) для Hotspot/USB-тетеринга,
     *    игнорируя сотовые интерфейсы (rmnet, ccmni, pdp, tun).
     */
    fun getLocalIpAddress(context: Context): String? {
        return runCatching {
            findActiveLocalIp(context) ?: findInterfaceIp()
        }.onFailure {
            Timber.e(it, "NetworkIpHelper: ошибка определения IP адреса")
        }.getOrNull()
    }

    private fun findActiveLocalIp(context: Context): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val activeNetwork = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return null
        val isLocalTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        if (!isLocalTransport) return null

        val linkProps = cm.getLinkProperties(activeNetwork) ?: return null
        for (linkAddress in linkProps.linkAddresses) {
            val addr = linkAddress.address
            if (addr is Inet4Address && !addr.isLoopbackAddress && addr.isSiteLocalAddress) {
                return addr.hostAddress
            }
        }
        return null
    }

    private fun findInterfaceIp(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.let { Collections.list(it) }.orEmpty()
        val prioritized = interfaces.sortedByDescending { getInterfacePriority(it.name.lowercase()) }

        for (nif in prioritized) {
            val name = nif.name.lowercase()
            val priority = getInterfacePriority(name)
            if (priority < 0 || !nif.isUp || nif.isLoopback) continue
            val addresses = nif.inetAddresses?.let { Collections.list(it) }.orEmpty()
            for (addr in addresses) {
                if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                    val host = addr.hostAddress
                    if (!host.isNullOrBlank()) return host
                }
            }
        }
        return null
    }

    private fun getInterfacePriority(name: String): Int {
        return when {
            name.startsWith("ap") || name.startsWith("softap") || name.startsWith("swlan") -> 4
            name.startsWith("wlan") -> 3
            name.startsWith("rndis") || name.startsWith("eth") -> 2
            isIgnoredInterface(name) -> -1
            else -> 0
        }
    }

    private fun isIgnoredInterface(name: String): Boolean {
        return name.startsWith("rmnet") ||
            name.startsWith("ccmni") ||
            name.startsWith("pdp") ||
            name.startsWith("wwan") ||
            name.startsWith("clat") ||
            name.startsWith("tun") ||
            name.startsWith("dummy")
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

    fun isValidPort(port: Int): Boolean = port in 1..65535

    fun isValidIpv4(ip: String?): Boolean {
        if (ip.isNullOrBlank()) return false
        val parts = ip.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            val num = part.toIntOrNull() ?: return@all false
            num in 0..255 && (part == "0" || !part.startsWith('0'))
        }
    }

    fun buildServerUrl(ip: String, port: Int): String {
        if (ip.isBlank() || !isValidPort(port)) return ""
        return "http://$ip:$port"
    }
}
