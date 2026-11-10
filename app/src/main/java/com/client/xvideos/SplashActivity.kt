package com.client.xvideos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.client.xvideos.calculator.LauncherAliasManager
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.duress.DuressTokenStore
import com.client.xvideos.common.rotation.RotationLockKeeper
import com.client.xvideos.common.settings.Settings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Стартовая activity со splash screen.
 *
 * Держит заставку на экране, пока подготавливаются локальные данные:
 * списки сохранённого R, блокировки, коллекции и временный cache DAO.
 * После инициализации передаёт управление `MainActivity`.
 */
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    @Inject
    lateinit var appStartupInitializer: AppStartupInitializer

    private var isReady = false

    /**
     * Запускает splash screen и фоновую инициализацию приложения.
     *
     * `setKeepOnScreenCondition` привязан к `isReady`, поэтому системная заставка
     * остаётся видимой до завершения `initApp()`.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        val duressToken = intent.getStringExtra(DuressTokenStore.EXTRA_DURESS_TOKEN)
        val duressUnlocked = DuressTokenStore.consumeTokenIfValid(this, duressToken)
        val isCamouflage = Settings.isInitialized && Settings.camouflage_calculator_enabled.field.value
        val isCamouflageActive = isCamouflage && !duressUnlocked

        // В режиме камуфляжа калькулятора заставку со стандартной анимацией загрузки
        // не ставим ни при каких условиях: калькулятор должен открываться сразу и без демаскировки.
        val splashScreen = if (!isCamouflageActive) {
            installSplashScreen()
        } else {
            null
        }

        super.onCreate(savedInstanceState)
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

        // держим сплэш пока инициализация не завершена
        splashScreen?.setKeepOnScreenCondition { !isReady }

        // Запускаем инициализацию
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    initApp()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "SplashActivity: ошибка при фоновой инициализации приложения")
            }
            isReady = true
            if (duressUnlocked) {
                AppLockSession.unlock()
            } else if (AppLockRepository.isEnabled(this@SplashActivity)) {
                AppLockSession.lock()
            }
            // Extra про необходимость замка сюда больше не кладём: MainActivity
            // сознательно определяет это сама через AppLockRepository.shouldShowLock,
            // чтобы прямой запуск Activity (напр. через adb) не обходил замок.
            //
            // Action, data и extras входящего intent дальше не передаются. Эта
            // Activity экспортирована, её запускает кто угодно с чем угодно, а
            // MainActivity закрыта от внешних вызовов — пересылка делала её
            // достижимой для чужих данных. Читать их там некому.
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
    }

    // Возврат системной блокировки поворота при уходе из приложения, см. RotationLockKeeper.
    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        RotationLockKeeper.onTopResumedActivityChanged(this, isTopResumedActivity)
    }

    /**
     * Подготавливает данные, которые нужны сразу после старта.
     */
    suspend fun initApp() {
        appStartupInitializer.init()
    }

}
