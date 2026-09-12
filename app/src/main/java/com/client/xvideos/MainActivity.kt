package com.client.xvideos

import com.client.xvideos.common.p2p.ui.P2pBackgroundOverlay
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.calculator.CalculatorScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockScreen
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.p2p.P2pPermissions
import com.client.xvideos.common.p2p.toggleP2pService
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.storage.StorageCleanupGate
import com.client.xvideos.common.util.KeepScreenOn
import com.client.xvideos.common.videoplayer.util.VideoDiskCacheCleaner
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.screenRoot.ScreenRoot
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        isAppMinimized = true
    }

    override fun onPause() {
        super.onPause()
        isAppMinimized = true
    }

    override fun onResume() {
        super.onResume()
        isAppMinimized = false
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

        val shouldShowAppLock = AppLockRepository.shouldShowLock(this)
        if (shouldShowAppLock) {
            window?.decorView?.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        launchStartupCleanup()

        setContent {
            var isAppLocked by rememberSaveable { mutableStateOf(shouldShowAppLock) }

            val lifecycleOwner = LocalLifecycleOwner.current
            LaunchedEffect(lifecycleOwner) {
                lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    if (AppLockRepository.shouldShowLock(this@MainActivity)) isAppLocked = true
                }
            }

            val blurRecentTasks = Settings.blur_recent_tasks.field.collectAsStateWithLifecycle().value
            val shouldBlur = (isAppMinimized && blurRecentTasks) || isAppLocked

            KeepScreenOn()
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
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (shouldBlur) {
                                            Modifier.blur(25.dp)
                                        } else {
                                            Modifier
                                        }
                                    )
                            ) {
                                ScreenRoot.Content()
                                P2pBackgroundOverlay()
                            }

                            if (isAppMinimized && blurRecentTasks) {
                                MinimizedPrivacyOverlay()
                            }

                            if (isAppLocked) {
                                AppLockOverlay(onUnlockSuccess = { isAppLocked = false })
                            }
                        }
                    }
                }
            }
        }
    }

    private fun configureWindow() {
        val currentWindow = this.window ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            currentWindow.isNavigationBarContrastEnforced = false
        }

        val windowInsetsController = WindowCompat.getInsetsController(currentWindow, currentWindow.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())

        currentWindow.attributes = currentWindow.attributes?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    private fun launchStartupCleanup() {
        lifecycleScope.launch(Dispatchers.IO) {
            storageCleanupGate.await()
            appFileDatabase.get().clearVolatileCachesOnProcessStart()
            appFileDatabase.get().deleteExpiredCaches()
            VideoDiskCacheCleaner.clearLegacyCaches(applicationContext)
            savedRed.nichesCache.refreshIfStale()

            if (Settings.p2p_background_receive.field.value && P2pPermissions.allGranted(applicationContext)) {
                toggleP2pService(applicationContext, true)
            }
        }
    }

    @Composable
    private fun MinimizedPrivacyOverlay() {
        val isCamouflage = Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle().value
        val scrimAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.45f else 0.88f
        Box(
            modifier = Modifier
                .fillMaxSize()
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

    @Composable
    private fun AppLockOverlay(onUnlockSuccess: () -> Unit) {
        val isCamouflage = Settings.camouflage_calculator_enabled.field.collectAsStateWithLifecycle().value
        if (isCamouflage) {
            CalculatorScreen(
                onUnlock = { password ->
                    if (AppLockRepository.lockoutRemainingMillis(this@MainActivity) > 0L) {
                        false
                    } else if (AppLockRepository.verifyPassword(this@MainActivity, password)) {
                        AppLockRepository.resetFailedAttempts(this@MainActivity)
                        AppLockSession.unlock()
                        onUnlockSuccess()
                        true
                    } else {
                        AppLockRepository.registerFailedAttempt(this@MainActivity)
                        false
                    }
                },
                onBack = { moveTaskToBack(true) }
            )
        } else {
            BackHandler { moveTaskToBack(true) }
            AppLockScreen(
                onUnlock = { password ->
                    if (AppLockRepository.verifyPassword(this@MainActivity, password)) {
                        AppLockSession.unlock()
                        onUnlockSuccess()
                        true
                    } else {
                        false
                    }
                }
            )
        }
    }
}
