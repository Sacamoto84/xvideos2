package com.client.xvideos

import com.client.xvideos.common.p2p.ui.P2pBackgroundOverlay
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.calculator.LauncherAliasManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.applock.LocalAppLockActive
import com.client.xvideos.common.applock.WindowPrivacy
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.p2p.P2pPermissions
import com.client.xvideos.common.p2p.toggleP2pService
import com.client.xvideos.common.rotation.RotationLockKeeper
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.storage.StorageCleanupGate
import com.client.xvideos.common.ui.statusbar.LocalStatusBarRequests
import com.client.xvideos.common.ui.statusbar.StatusBarRequests
import com.client.xvideos.common.videoplayer.util.VideoDiskCacheCleaner
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.screenRoot.ScreenRoot
import com.client.xvideos.screenRoot.molecule.StatusBarHost
import com.client.xvideos.ui.theme.XvideosTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Базовый URL для стартовой точки работы с основным сайтом.
 */

/**
 * Главная activity приложения.
 *
 * Отвечает за:
 * - настройку edge-to-edge режима и системных панелей;
 * - показ замка приложения, если он включён;
 * - инициализацию кеша видеоплеера;
 * - отображение корневого Compose-интерфейса приложения.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity()//, ImageLoaderFactory
{
    @Inject
    lateinit var savedRed: SavedRed

    @Inject
    lateinit var appFileDatabase: javax.inject.Provider<AppFileDatabase>

    @Inject
    lateinit var storageCleanupGate: StorageCleanupGate

    private var isAppMinimized by mutableStateOf(false)

    /**
     * Сеанс закрыт замком. Источник правды — `AppLockSession` в памяти процесса,
     * поэтому значение перечитывается в `onCreate`, `onStart`, `onResume` и по
     * событию «сеанс закрыли», а не хранится в сохранённом состоянии Activity: оно переживает гибель процесса,
     * и система восстанавливала оттуда «замок снят» — первый кадр после возврата
     * из «Недавних» рисовал содержимое без кода.
     *
     * Сам экран замка — [LockActivity]. Здесь по этому флагу содержимое только
     * прячется: оно остаётся в композиции под чёрной заглушкой, чтобы после
     * ввода кода открыться сразу.
     */
    private var isAppLocked by mutableStateOf(false)

    /** Последнее решение `StatusBarHost`: переприменяется в `onResume`. */
    private var statusBarVisible = false

    private fun applyStatusBarVisibility(visible: Boolean) {
        statusBarVisible = visible
        window?.setStatusBarVisible(visible)
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Сеанс закрыли, пока Activity на экране. Вызова жизненного цикла при этом
     * может не быть вовсе: под полупрозрачным системным окном `MainActivity`
     * стоит на паузе, повторный запуск приложения закрывает сеанс, новая
     * заставка тут же уходит — и `MainActivity` остаётся видимой, как была
     * (проверено на телефоне с Android 16). Событие приходит на потоке, который
     * закрыл сеанс, поэтому показ замка уходит на главный.
     */
    private val onSessionLocked = AppLockSession.OnLockedListener {
        mainHandler.post {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) showLockScreenIfLocked()
        }
    }

    /**
     * Показывает замок, пока сеанс закрыт. Зовётся отовсюду, где сеанс мог
     * оказаться закрытым при видимой Activity: `onStart`, `onResume`, событие
     * «сеанс закрыли». Вызовы приходят вплотную, а `SINGLE_TOP` второй запуск
     * с первым не объединяет, пока первый экран не создан, — поэтому запускает
     * тот, кто первым получил разрешение у [LockScreenPresence].
     */
    private fun showLockScreenIfLocked() {
        isAppLocked = AppLockRepository.shouldShowLock(this)
        if (!isAppLocked) return
        if (!LockScreenPresence.claim(taskId, SystemClock.elapsedRealtime())) return
        startActivity(
            Intent(this, LockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        )
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        isAppMinimized = true
        val isCamouflage = Settings.isInitialized && Settings.camouflage_calculator_enabled.field.value
        val isLockShown = AppLockRepository.shouldShowLock(this)
        if (isCamouflage && isLockShown) return

        if (Settings.isInitialized && Settings.blur_recent_tasks.field.value) {
            window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onPause() {
        super.onPause()
        isAppMinimized = true
        val isCamouflage = Settings.isInitialized && Settings.camouflage_calculator_enabled.field.value
        val isLockShown = AppLockRepository.shouldShowLock(this)
        val needSecure = WindowPrivacy.needsSecureFlag(
            isLockShown = isLockShown,
            isMinimized = true,
            blurRecentTasks = Settings.isInitialized && Settings.blur_recent_tasks.field.value,
            lockEnabled = AppLockRepository.isEnabled(this),
            isCamouflage = isCamouflage,
        )
        if (needSecure) {
            window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    /**
     * Activity уже видна, но может так и не выйти на передний план: поверх стоит
     * полупрозрачное системное окно — запрос разрешения, выбор приложения для
     * ссылки. `onResume` тогда не приходит, пока это окно не закроют. Пока замок
     * проверялся только в нём, после возврата в приложение с истёкшим сроком
     * сеанса экран замка под таким окном не появлялся: на сам запрос можно было
     * ответить без кода, а при выключенной «Защите в диспетчере задач» под ним
     * было видно и содержимое.
     */
    override fun onStart() {
        super.onStart()
        AppLockSession.addOnLockedListener(onSessionLocked)
        showLockScreenIfLocked()
    }

    override fun onStop() {
        super.onStop()
        AppLockSession.removeOnLockedListener(onSessionLocked)
    }

    // Возврат системной блокировки поворота при уходе из приложения, см. RotationLockKeeper.
    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        RotationLockKeeper.onTopResumedActivityChanged(this, isTopResumedActivity)
    }

    override fun onResume() {
        super.onResume()
        isAppMinimized = false
        val isCamouflage = Settings.isInitialized && Settings.camouflage_calculator_enabled.field.value
        val isLockShown = AppLockRepository.shouldShowLock(this)
        val needSecure = WindowPrivacy.needsSecureFlag(
            isLockShown = isLockShown,
            isMinimized = false,
            blurRecentTasks = Settings.isInitialized && Settings.blur_recent_tasks.field.value,
            lockEnabled = AppLockRepository.isEnabled(this),
            isCamouflage = isCamouflage,
        )
        if (!needSecure) {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        applyStatusBarVisibility(statusBarVisible)
        showLockScreenIfLocked()
    }

    /**
     * Инициализирует окно, скрывает системные панели и поднимает корневой
     * Compose UI.
     */
    @OptIn(ExperimentalVoyagerApi::class, ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(0xFF212121.toInt()),
        )
        super.onCreate(savedInstanceState)

        configureWindow()
        LauncherAliasManager.updateTaskDescription(this, Settings.camouflage_calculator_enabled.field.value)

        // До первого кадра: содержимое обязано быть спрятано раньше, чем onResume
        // запустит экран замка.
        isAppLocked = AppLockRepository.shouldShowLock(this)

        launchStartupCleanup()

        setContent {
            val statusBarRequests = remember { StatusBarRequests() }

            val isCamouflage by Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle()
            LaunchedEffect(isCamouflage) {
                LauncherAliasManager.updateTaskDescription(this@MainActivity, isCamouflage)
            }

            val blurRecentTasks by Settings.blur_recent_tasks.field.collectAsStateWithLifecycle()
            val shouldBlur = (isAppMinimized && blurRecentTasks) || isAppLocked

            // SECURITY: динамическая аппаратная защита превью в Recent Apps через FLAG_SECURE
            val appLockEnabled by Settings.app_lock_enabled.field.collectAsStateWithLifecycle()
            LaunchedEffect(isAppMinimized, blurRecentTasks, isAppLocked, appLockEnabled, isCamouflage) {
                val needSecure = WindowPrivacy.needsSecureFlag(
                    isLockShown = isAppLocked,
                    isMinimized = isAppMinimized,
                    blurRecentTasks = blurRecentTasks,
                    lockEnabled = appLockEnabled && AppLockRepository.isEnabled(this@MainActivity),
                    isCamouflage = isCamouflage,
                )
                if (needSecure) {
                    window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            // SECURITY: Отключение P2P радиоэфира и приема файлов при активном замке
            val p2pBackgroundReceive by Settings.p2p_background_receive.field.collectAsStateWithLifecycle()
            LaunchedEffect(isAppLocked, p2pBackgroundReceive) {
                if (isAppLocked) {
                    toggleP2pService(this@MainActivity, false)
                } else if (p2pBackgroundReceive && P2pPermissions.allGranted(this@MainActivity)) {
                    toggleP2pService(this@MainActivity, true)
                }
            }

            XvideosTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121))
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                            .background(Color.Black)
                            .semantics { testTagsAsResourceId = true },
                        color = Color.Black,
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val shouldHideProtectedContent = isAppLocked || (isAppMinimized && blurRecentTasks)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (shouldHideProtectedContent) {
                                            Modifier
                                                .clearAndSetSemantics { }
                                                .drawWithContent {
                                                    // Глушим отрисовку пикселей и очищаем Semantics-дерево
                                                    // для защиты от Accessibility Services и UI Automator
                                                }
                                        } else if (shouldBlur) {
                                            Modifier.blur(25.dp)
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                CompositionLocalProvider(
                                    LocalStatusBarRequests provides statusBarRequests,
                                    LocalAppLockActive provides (isAppLocked || (isAppMinimized && blurRecentTasks)),
                                ) {
                                    ScreenRoot.Content()
                                }
                                P2pBackgroundOverlay()
                                StatusBarHost(
                                    hideRequests = statusBarRequests.hideRequests,
                                    onVisibleChange = ::applyStatusBarVisibility,
                                )
                            }

                            if (isAppMinimized && blurRecentTasks) {
                                MinimizedPrivacyOverlay()
                            }

                            if (isAppLocked) {
                                // Заглушка на то время, пока поверх открывается
                                // LockActivity, и на случай, если она не открылась.
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {}
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun configureWindow() {
        val currentWindow = this.window ?: return
        currentWindow.applyAppWindowStyle()
        applyStatusBarVisibility(hasTopCutout())

        // Окно закрыто от системной службы автозаполнения целиком: текст поиска
        // ей отдавать незачем. Раньше это получалось побочно и только при
        // включённом замке — строка стояла здесь ради экрана замка и уехала
        // вместе с ним в LockActivity, после чего поля снова стали ей видны.
        // Запрет действует на всех потомков корня; экран, которому менеджер
        // паролей нужен (форма входа L), снимает его на время своего показа —
        // см. AllowWindowAutofill. Диалоги — отдельные окна, на них он не
        // распространяется.
        currentWindow.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS

        val isCamouflageAtStart = Settings.isInitialized && Settings.camouflage_calculator_enabled.field.value
        val isLockShownAtStart = AppLockRepository.shouldShowLock(this)
        val needSecureAtStart = WindowPrivacy.needsSecureFlag(
            isLockShown = isLockShownAtStart,
            isMinimized = false,
            blurRecentTasks = Settings.isInitialized && Settings.blur_recent_tasks.field.value,
            lockEnabled = AppLockRepository.isEnabled(this),
            isCamouflage = isCamouflageAtStart,
        )
        if (needSecureAtStart) {
            currentWindow.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun launchStartupCleanup() {
        lifecycleScope.launch(Dispatchers.IO) {
            storageCleanupGate.await()
            appFileDatabase.get().clearVolatileCachesOnProcessStart()
            appFileDatabase.get().deleteExpiredCaches()
            VideoDiskCacheCleaner.clearLegacyCaches(applicationContext)
            savedRed.nichesCache.refreshIfStale()

            if (!AppLockRepository.shouldShowLock(applicationContext) &&
                Settings.p2p_background_receive.field.value &&
                P2pPermissions.allGranted(applicationContext)
            ) {
                toggleP2pService(applicationContext, true)
            }
        }
    }

    @Composable
    private fun MinimizedPrivacyOverlay() {
        val isCamouflage by Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle()
        val scrimAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.45f else 1.0f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .background(Color.Black.copy(alpha = scrimAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    if (isCamouflage) R.drawable.ic_launcher_calculator
                    else R.drawable.icon_red
                ),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
