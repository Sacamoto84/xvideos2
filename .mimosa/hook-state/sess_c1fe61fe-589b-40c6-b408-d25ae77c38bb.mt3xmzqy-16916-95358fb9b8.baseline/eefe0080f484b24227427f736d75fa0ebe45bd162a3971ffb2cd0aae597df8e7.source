package com.client.xvideos.common.p2p

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Набор и проверка runtime-разрешений для Nearby в зависимости от версии Android. */
object P2pPermissions {

    fun required(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val list = mutableListOf(
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+: обнаружение идёт через NEARBY_WIFI_DEVICES,
                // геолокация Nearby не нужна и в манифесте обрезана по API 32.
                list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            } else {
                list.add(Manifest.permission.ACCESS_FINE_LOCATION)
                list.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            list.toTypedArray()
        }
        else -> arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    fun allGranted(context: Context): Boolean = required().all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
