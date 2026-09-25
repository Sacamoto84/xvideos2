package com.client.xvideos.calculator

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber

/**
 * Управляет переключением значка и имени приложения на рабочем столе Android.
 *
 * Переключает видимость между стандартной [com.client.xvideos.SplashActivity]
 * и псевдонимом `CalculatorAlias`.
 */
object LauncherAliasManager {

    private const val SPLASH_ACTIVITY = "com.client.xvideos.SplashActivity"
    private const val CALCULATOR_ALIAS = "com.client.xvideos.CalculatorAlias"

    fun setCalculatorAliasEnabled(context: Context, enabled: Boolean) {
        val packageManager = context.packageManager
        val splashComponent = ComponentName(context, SPLASH_ACTIVITY)
        val calculatorComponent = ComponentName(context, CALCULATOR_ALIAS)

        try {
            val currentCalculatorState = packageManager.getComponentEnabledSetting(calculatorComponent)
            val isAlreadyEnabled = currentCalculatorState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            if (enabled == isAlreadyEnabled) return

            if (enabled) {
                // Сначала включаем псевдоним калькулятора, затем отключаем основной сплэш
                packageManager.setComponentEnabledSetting(
                    calculatorComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                packageManager.setComponentEnabledSetting(
                    splashComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } else {
                // Сначала включаем основной сплэш, затем отключаем псевдоним калькулятора
                packageManager.setComponentEnabledSetting(
                    splashComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                packageManager.setComponentEnabledSetting(
                    calculatorComponent,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Не удалось переключить состояние launcher alias")
        }
    }
}
