package com.client.xvideos.common.rotation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.Surface
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/**
 * [RotationSettings] поверх системных настроек `ACCELEROMETER_ROTATION` и `USER_ROTATION`.
 *
 * Читать их может любое приложение, писать — только с правом «Изменение системных
 * настроек» (`WRITE_SETTINGS`), которое пользователь включает сам на системном экране
 * ([openPermissionScreen]).
 */
class SystemRotationSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : RotationSettings {

    override fun isAutoRotateOn(): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1

    override fun lockedRotation(): Int =
        Settings.System.getInt(context.contentResolver, Settings.System.USER_ROTATION, Surface.ROTATION_0)

    override fun canWrite(): Boolean = Settings.System.canWrite(context)

    override fun lock(rotation: Int) {
        // Право могут отозвать между проверкой и записью: тогда блокировка просто не вернётся.
        try {
            Settings.System.putInt(context.contentResolver, Settings.System.USER_ROTATION, rotation)
            Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
        } catch (e: SecurityException) {
            Timber.w(e, "SystemRotationSettings: нет права вернуть блокировку поворота")
        }
    }

    companion object {

        /** Открывает системный экран права «Изменение системных настроек» для приложения. */
        fun openPermissionScreen(context: Context) {
            val packageUri = "package:${context.packageName}".toUri()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, packageUri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Timber.w(e, "SystemRotationSettings: нет экрана права, открываю сведения о приложении")
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
