package com.client.xvideos

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.PermissionScreenActivity.PermissionStorage
import com.client.xvideos.common.applock.AppLockRepository
import com.client.xvideos.common.applock.AppLockScreen
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.eventBus.Event
import com.client.xvideos.common.eventBus.EventBus
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.p2p.P2pPermissions
import com.client.xvideos.common.p2p.toggleP2pService
import com.client.xvideos.common.p2p.ui.ScreenP2pReceive
import com.client.xvideos.common.settings.Settings
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.KeepScreenOn
import com.client.xvideos.common.util.getStatusBarInsetDp
import com.client.xvideos.common.util.getTopInsetDp
import com.client.xvideos.common.videoplayer.util.VideoDiskCacheCleaner
import com.client.xvideos.l.ui.screens.explorer.L_ScreenExplorer
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.ui.root.R_Screen_Root
import com.client.xvideos.screenRoot.ScreenRoot
import com.client.xvideos.ui.theme.XvideosTheme
import com.client.xvideos.x.screens.dashboards.ScreenXDashBoards
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
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
 * - проверку необходимых разрешений перед запуском основного UI;
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
     * Инициализирует окно, скрывает системные панели, проверяет разрешения
     * и поднимает корневой Compose UI.
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

        if (!PermissionStorage.hasPermissions(this)) {
            val intent = Intent(this, PermissionScreenActivity::class.java)
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
            return
        }

        // SECURITY: показ замка определяется ИСКЛЮЧИТЕЛЬНО состоянием блокировки.
        // Нельзя завязываться на intent-extra от вызывающего: даже если Activity
        // запустят напрямую (напр. `adb am start`) без extra, замок обязан показаться.
        val shouldShowAppLock = AppLockRepository.shouldShowLock(this)
        if (shouldShowAppLock) {
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        lifecycleScope.launch(Dispatchers.IO) {
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

//                    Navigator(MenuScreen, key = "root_navigator") { navigator ->
//                        CurrentScreen()
//                    }
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

@Composable
private fun P2pBackgroundOverlay() {
    val event by EventBus.events.collectAsState(null)
    var visible by rememberSaveable { mutableStateOf(false) }
    var text by rememberSaveable { mutableStateOf("") }
    var progress by remember { mutableStateOf(0f) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var isSuccess by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(event) {
        when (val e = event) {
            is Event.P2pTransferUpdate.Progress -> {
                visible = true
                isError = false
                isSuccess = false
                text = "Приём от ${e.endpointName}"
                progress = if (e.total > 0) e.transferred.toFloat() / e.total else 0f
            }
            is Event.P2pTransferUpdate.Success -> {
                visible = true
                isError = false
                isSuccess = true
                text = "Получено от ${e.endpointName} ✓"
                progress = 1f
                delay(3000)
                visible = false
            }
            is Event.P2pTransferUpdate.Error -> {
                visible = true
                isError = true
                isSuccess = false
                text = "Ошибка приёма: ${e.message}"
                delay(5000)
                visible = false
            }
            else -> {}
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // Бары спрятаны (hide(systemBars)), их инсет всегда 0 — отступаем от выреза камеры.
                .displayCutoutPadding()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (isError) Theme.L.r0 else if (isSuccess) Theme.L.g0 else Color(0xFF2C2C2C),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                if (!isError && !isSuccess) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

/**
 * Стартовый экран выбора раздела приложения.
 *
 * Предоставляет быстрый переход к основным источникам контента.
 */
object MenuScreen : Screen {

    private fun readResolve(): Any = MenuScreen

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
                    IconButton(onClick = { navigator.push(AppSettingsScreen) }, modifier = Modifier.displayCutoutPadding().size(48.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp))
                    }
                    // Демо-экран виброоткликов (HapticFeedbackType) для тестов
                    IconButton(
                        onClick = { navigator.push(HapticDemoScreen) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .displayCutoutPadding()
                            .size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Vibration,
                            contentDescription = "Haptic demo",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp))
                    }

                    // Приём item по P2P (Nearby)
                    IconButton(
                        onClick = { navigator.push(ScreenP2pReceive()) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .displayCutoutPadding()
                            .size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = "Приём P2P",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp))
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF353535)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {

                ButtonSelect(R.drawable.icon_xvideos_white) {
                    navigator.push(ScreenXDashBoards())
                }

                ButtonSelect(R.drawable.icon_luscious, "buttonL") {
                    navigator.push(L_ScreenExplorer()) // или ScreenLusciousRoot()
                }
                ButtonSelect(R.drawable.icon_red) {
                    navigator.push(R_Screen_Root()) // или ScreenRedRoot()
                }

            }
        }
    }
}


/**
 * Универсальная кнопка выбора раздела с иконкой.
 *
 * @param iconId ресурс drawable, отображаемый внутри кнопки.
 * @param tag optional test tag для UI-тестов.
 * @param onClick callback, вызываемый при нажатии.
 */
@Composable
private fun ButtonSelect(iconId: Int, tag : String= "", onClick: () -> Unit) {

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFF565656), RoundedCornerShape(16.dp))
            .background(Color(0xFF212121))
            .clickable { onClick() }
            .padding(vertical = 16.dp)
            .then(
                if (tag.isNotEmpty()) {
                    Modifier.testTag(tag)
                } else Modifier
            )

        ,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(iconId),
            contentDescription = null,
            modifier = Modifier.height(80.dp),
            contentScale = ContentScale.FillHeight
        )
    }

}

/**
 * Preview стартового меню для быстрой проверки в Compose Preview.
 */
@Preview(device = "id:pixel_9_pro")
@Composable
private fun MenuPreview() {
    MenuScreen.Content()
}
