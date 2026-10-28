package com.client.xvideos

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.client.xvideos.applock.molecule.AppLockGate
import com.client.xvideos.calculator.LauncherAliasManager
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.applock.AppLockUnlockResult
import com.client.xvideos.common.applock.WindowPrivacy
import com.client.xvideos.duress.DuressLauncher
import com.client.xvideos.common.rotation.RotationLockKeeper
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.ui.theme.XvideosTheme
import timber.log.Timber

/**
 * Экран замка — отдельная Activity поверх [MainActivity].
 *
 * Раньше замок был слоем внутри окна `MainActivity`, и это обходилось двумя
 * способами. Диалоги, выпадающие меню и всплывающие панели — отдельные окна:
 * система рисует их над окном Activity, так что открытое меню оставалось поверх
 * замка, читалось и нажималось. А содержимое под слоем не удалялось из
 * композиции — Tab с внешней клавиатуры уводил фокус под замок, Enter нажимал.
 *
 * Непрозрачная Activity сверху закрывает оба пути средствами системы: окна
 * нижней Activity скрываются вместе с ней, а нажатия клавиш уходят в верхнее
 * окно. `MainActivity` запускает этот экран из `onResume`, пока сеанс закрыт, и
 * на это время сама ничего не показывает.
 */
class LockActivity : ComponentActivity() {

    /** См. [AppLockRepository.isSecretLost]. Перечитывается при каждом возврате на экран. */
    private var secretLost by mutableStateOf(false)

    private val isCamouflage: Boolean
        get() = Settings.isInitialized && Settings.camouflage_calculator_enabled.field.value

    /**
     * Задача, в которой этот экран учтён в [LockScreenPresence] как показанный.
     * Запоминается при учёте: к `onDestroy` Activity может уже не числиться в
     * задаче, и `taskId` тогда вернёт не её — экран остался бы «показанным»
     * навсегда, и замок в этой задаче больше не запустился бы.
     */
    private var countedInTask: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(0xFF212121.toInt()),
        )
        super.onCreate(savedInstanceState)
        if (finishIfUnlocked()) {
            LockScreenPresence.onRequestDropped(taskId)
            return
        }
        countedInTask = taskId.also(LockScreenPresence::onShown)

        window.applyAppWindowStyle()
        window.setStatusBarVisible(hasTopCutout())
        window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        hideForeignOverlays()
        secretLost = AppLockRepository.isSecretLost(this)
        applyPrivacy()

        val showCalculator = isCamouflage
        setContent {
            XvideosTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121))
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                        color = Color.Black,
                    ) {
                        AppLockGate(
                            isCamouflage = showCalculator,
                            secretLost = secretLost,
                            onUnlock = ::tryUnlock,
                            onUnlockFailed = { AppLockRepository.registerFailedAttempt(this@LockActivity) },
                            onBack = { moveTaskToBack(true) },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (finishIfUnlocked()) return
        secretLost = AppLockRepository.isSecretLost(this)
        applyPrivacy()
    }

    // Возврат системной блокировки поворота при уходе из приложения, см. RotationLockKeeper.
    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        RotationLockKeeper.onTopResumedActivityChanged(this, isTopResumedActivity)
    }

    /**
     * Проверяет код и при успехе снимает замок. Блокировку ввода проверяет сам:
     * калькулятор её не показывает и присылает сюда каждое нажатие «=».
     */
    private suspend fun tryUnlock(code: String): Boolean {
        if (AppLockRepository.lockoutRemainingMillis(this) > 0L) return false
        return when (AppLockRepository.verifyUnlockCode(this, code)) {
            is AppLockUnlockResult.AccessGranted -> {
                AppLockRepository.resetFailedAttempts(this)
                AppLockSession.unlock()
                finishWithoutAnimation()
                true
            }
            is AppLockUnlockResult.DuressTriggered -> {
                AppLockRepository.resetFailedAttempts(this)
                val success = DuressLauncher.execute(this)
                if (!success) {
                    // Если изоляция не удалась, отвечаем как на неверный код (спека, пункт 8)
                    false
                } else {
                    true
                }
            }
            is AppLockUnlockResult.Wrong -> {
                false
            }
        }
    }

    /** Сеанс уже открыт — показывать нечего. */
    private fun finishIfUnlocked(): Boolean {
        if (AppLockRepository.shouldShowLock(this)) return false
        finishWithoutAnimation()
        return true
    }

    override fun onDestroy() {
        markGone()
        super.onDestroy()
    }

    /**
     * Снимает экран с учёта уже при закрытии, не дожидаясь `onDestroy`: тот
     * приходит с задержкой, а сеанс за это время могут закрыть снова — и новый
     * экран замка должен запуститься, а не счесть уходящий показанным.
     */
    private fun markGone() {
        val task = countedInTask ?: return
        countedInTask = null
        LockScreenPresence.onGone(task)
    }

    private fun finishWithoutAnimation() {
        markGone()
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    /**
     * Карточка в «Недавних» и снимки экрана. Калькулятор снимается как есть —
     * чёрная карточка выдала бы, что это не калькулятор; форма кода закрыта
     * `FLAG_SECURE`.
     */
    private fun applyPrivacy() {
        LauncherAliasManager.updateTaskDescription(this, isCamouflage)
        val needSecure = WindowPrivacy.needsSecureFlag(
            isLockShown = true,
            isMinimized = false,
            blurRecentTasks = Settings.isInitialized && Settings.blur_recent_tasks.field.value,
            lockEnabled = true,
            isCamouflage = isCamouflage && !secretLost,
        )
        if (needSecure) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    /**
     * Убирает с экрана окна других приложений, нарисованные поверх замка.
     * Приложение с разрешением «поверх других окон» иначе может накрыть поле
     * кода своей копией и собрать введённое. До Android 12 такого запрета у
     * системы нет.
     */
    private fun hideForeignOverlays() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching { window.setHideOverlayWindows(true) }
            .onFailure { Timber.w(it, "LockActivity: окна поверх замка скрыть не удалось") }
    }
}
