package com.client.xvideos

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.client.xvideos.common.applock.AppLockSession
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.util.KeepScreenOn
import com.client.xvideos.common.videoplayer.util.VideoDiskCacheCleaner
import com.client.xvideos.l.ui.screens.explorer.L_ScreenExplorer
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.ui.root.R_Screen_Root
import com.client.xvideos.screenRoot.ScreenRoot
import com.client.xvideos.x.screens.dashboards.ScreenXDashBoards
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

    companion object {
        internal const val EXTRA_REQUIRE_APP_LOCK = "com.client.xvideos.EXTRA_REQUIRE_APP_LOCK"
    }

    /**
     * Инициализирует окно, скрывает системные панели, проверяет разрешения
     * и поднимает корневой Compose UI.
     */
    @OptIn(ExperimentalVoyagerApi::class, ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        //enableEdgeToEdge(statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT))
        super.onCreate(savedInstanceState)

        val window = this.window

        val windowInsetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        window?.let { WindowCompat.setDecorFitsSystemWindows(window, false) }
        window?.attributes = window.attributes?.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())


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
        // Extra из SplashActivity оставлен лишь как подсказка, но не как условие.
        val shouldShowAppLock = AppLockRepository.shouldShowLock(this)
        if (shouldShowAppLock) {
            window.decorView.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }

        lifecycleScope.launch(Dispatchers.IO) {
            appFileDatabase.get().clearVolatileCachesOnProcessStart()
            VideoDiskCacheCleaner.clearLegacyCaches(applicationContext)
            savedRed.nichesCache.refreshIfStale()
        }

        setContent {
            var isAppLocked by rememberSaveable { mutableStateOf(shouldShowAppLock) }

            KeepScreenOn()
            XvideosTheme(darkTheme = true) {
                //EdgeToEdgeFix()

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .semantics { testTagsAsResourceId = true }
                    //.windowInsetsPadding(WindowInsets.ime)
                    //.consumeWindowInsets(WindowInsets.ime)
                    //.displayCutoutPadding()
                    //.systemBarsPadding())
                )
                {

//                    Navigator(MenuScreen, key = "root_navigator") { navigator ->
//                        CurrentScreen()
//                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                        ScreenRoot.Content()

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

/**
 * Вспомогательный composable для дополнительной коррекции edge-to-edge поведения.
 *
 * Принудительно обновляет insets и скрывает status bar, когда это необходимо.
 */
@Composable
fun EdgeToEdgeFix() {
    val context = LocalContext.current
    val window = (context as? Activity)?.window ?: return
    val controller = WindowCompat.getInsetsController(window, window.decorView)

    LaunchedEffect(Unit) {
        // Поведение: показывать панели при свайпе
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // Не скрываем навигацию — только статус-бар (если нужно)
        controller.hide(WindowInsetsCompat.Type.statusBars())

        // "Пинаем" для корректного обновления insets
        window.decorView.requestApplyInsets()
    }
}
