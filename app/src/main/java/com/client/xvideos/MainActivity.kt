package com.client.xvideos

import com.client.xvideos.common.p2p.ui.P2pBackgroundOverlay
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Surface
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
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockScreen
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.p2p.P2pPermissions
import com.client.xvideos.common.p2p.toggleP2pService
import com.client.xvideos.common.settings.Settings
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
const val urlStart = "https://www.xv-ru.com"

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

    /**
     * Инициализирует окно, скрывает системные панели и поднимает корневой
     * Compose UI.
     */
    @OptIn(ExperimentalVoyagerApi::class, ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        // Edge-to-edge: на 26-28 красит navigationBarColor, на 29-34 дополнительно
        // снимает contrast-scrim, на 35+ цвет задаёт приложение (корневой Box в setContent).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(0xFF212121.toInt()),
        )
        super.onCreate(savedInstanceState)

        val window = this.window

        val windowInsetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Статус-бар скрыт всегда (раньше это делал windowFullscreen из темы)
        windowInsetsController?.hide(WindowInsetsCompat.Type.statusBars())

        window?.attributes = window.attributes?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        // SECURITY: показ замка определяется ИСКЛЮЧИТЕЛЬНО состоянием блокировки.
        // Нельзя завязываться на intent-extra от вызывающего: даже если Activity
        // запустят напрямую (напр. `adb am start`) без extra, замок обязан показаться.
        val shouldShowAppLock = AppLockRepository.shouldShowLock(this)
        if (shouldShowAppLock) {
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // Уборка staging-папок идёт в фоне с App.onCreate. Ждём её: ниже
            // стартует приём P2P, который пишет в inbox, а уборка этот каталог
            // пересоздаёт.
            App.instance.awaitStorageCleanup()

            appFileDatabase.get().clearVolatileCachesOnProcessStart()
            VideoDiskCacheCleaner.clearLegacyCaches(applicationContext)
            savedRed.nichesCache.refreshIfStale()
            
            // Запуск P2P сервиса если включен в настройках
            if (Settings.p2p_background_receive.field.value && P2pPermissions.allGranted(applicationContext)) {
                toggleP2pService(applicationContext, true)
            }
        }

        setContent {

            //val topInset = getTopInsetDp()
            //val topStatusBar = getStatusBarInsetDp()

            //Timber.i("QQQ $topInset statusbar:$topStatusBar")

            var isAppLocked by rememberSaveable { mutableStateOf(shouldShowAppLock) }

            KeepScreenOn()
            XvideosTheme(darkTheme = true) {

                // Подложка: на API 35+ системный бар прозрачный, полосу #212121 под
                // кнопками рисует это приложение. tappableElement снизу = высота
                // кнопочного бара, 0 на жестовой навигации.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF212121))
                ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.tappableElement.only(WindowInsetsSides.Bottom))
                        .background(Color.Black)
                        .semantics { testTagsAsResourceId = true }
                )
                {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ScreenRoot.Content()
                        P2pBackgroundOverlay()

                        if (isAppLocked) {
                            BackHandler { moveTaskToBack(true) }
                            AppLockScreen(
                                onUnlock = { password ->
                                    if (AppLockRepository.verifyPassword(this@MainActivity, password)) {
                                        AppLockSession.unlock()
                                        isAppLocked = false
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )
                        }
                    }

                }
                } // Box-подложка
            }
        }
    }
}
