package com.client.xvideos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.client.xvideos.calculator.LauncherAliasManager
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.duress.DuressTokenStore
import com.client.xvideos.common.rotation.RotationLockKeeper
import com.client.xvideos.common.settings.Settings

/** Передаёт управление главному окну или калькулятору. */
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val duressToken = intent.getStringExtra(DuressTokenStore.EXTRA_DURESS_TOKEN)
        val duressUnlocked = DuressTokenStore.consumeTokenIfValid(this, duressToken)
        val isCamouflage = Settings.isInitialized && Settings.camouflage_calculator_enabled.field.value
        val isCamouflageActive = isCamouflage && !duressUnlocked

        // В режиме камуфляжа калькулятора заставку со стандартной анимацией загрузки
        // не ставим ни при каких условиях: калькулятор должен открываться сразу и без демаскировки.
        val splashScreen = if (!isCamouflageActive) installSplashScreen() else null
        super.onCreate(savedInstanceState)
        splashScreen?.setKeepOnScreenCondition { true }
        LauncherAliasManager.updateTaskDescription(this, isCamouflage)
        val blurRecentTasks = Settings.isInitialized && Settings.blur_recent_tasks.field.value
        if (blurRecentTasks && !isCamouflageActive) {
            window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        // Возврат в уже запущенное приложение по иконке лаунчера.
        if (!isTaskRoot) {
            if (AppLockRepository.isEnabled(this)) AppLockSession.lock()
            finish()
            return
        }

        if (isCamouflageActive) {
            AppLockSession.lock()
            startActivity(
                Intent(this, LockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            )
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            finish()
            return
        }

        if (duressUnlocked) {
            AppLockSession.unlock()
        } else if (AppLockRepository.isEnabled(this)) {
            AppLockSession.lock()
        }
        // Входящий Intent экспортированного лаунчера не передаётся в MainActivity.
        startActivity(Intent(this, MainActivity::class.java))
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    // Возврат системной блокировки поворота при уходе из приложения, см. RotationLockKeeper.
    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        RotationLockKeeper.onTopResumedActivityChanged(this, isTopResumedActivity)
    }

}
