package com.client.xvideos.screenRoot

import com.client.xvideos.R
import com.client.xvideos.HapticDemoScreen
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.p2p.ui.ScreenP2pReceive
import com.client.xvideos.screenSettings.AppSettingsScreen
import com.client.xvideos.l.ui.screens.explorer.L_ScreenExplorer
import com.client.xvideos.r.ui.root.R_Screen_Root
import com.client.xvideos.x.screens.dashboards.ScreenXDashBoards

private val menuWindowInsets = WindowInsets(0, 0, 0, 0)
private val menuBackgroundColor = Color(0xFF353535)
private val menuButtonShape = RoundedCornerShape(16.dp)
private val menuButtonBorderColor = Color(0xFF565656)
private val menuButtonBgColor = Color(0xFF212121)

/**
 * Стартовый экран выбора раздела приложения.
 *
 * Предоставляет быстрый переход к основным источникам контента.
 */
object MenuScreen : Screen {

    private fun readResolve(): Any = MenuScreen

    override val key: ScreenKey = "MenuScreen"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val onOpenSettings = remember(navigator) { { navigator.push(AppSettingsScreen) } }
        val onOpenHapticDemo = remember(navigator) { { navigator.push(HapticDemoScreen) } }
        val onOpenP2pReceive = remember(navigator) { { navigator.push(ScreenP2pReceive()) } }
        val onOpenX = remember(navigator) { { navigator.push(ScreenXDashBoards()) } }
        val onOpenL = remember(navigator) { { navigator.push(L_ScreenExplorer()) } }
        val onOpenR = remember(navigator) { { navigator.push(R_Screen_Root()) } }

        Scaffold(
            contentWindowInsets = menuWindowInsets,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top)),
                    contentAlignment = Alignment.TopStart
                ) {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Настройки",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp))
                    }
                    // Демо-экран виброоткликов (HapticFeedbackType) для тестов
                    IconButton(
                        onClick = onOpenHapticDemo,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
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
                        onClick = onOpenP2pReceive,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
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
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(menuBackgroundColor)
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {

                ButtonSelect(R.drawable.icon_xvideos_white, onClick = onOpenX)

                ButtonSelect(R.drawable.icon_luscious, "buttonL", onClick = onOpenL)
                ButtonSelect(R.drawable.icon_red, onClick = onOpenR)

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
private fun ButtonSelect(iconId: Int, tag: String = "", onClick: () -> Unit) {
    val baseModifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()
        .clip(menuButtonShape)
        .border(2.dp, menuButtonBorderColor, menuButtonShape)
        .background(menuButtonBgColor)
        .clickable(onClick = onClick)
        .padding(vertical = 16.dp)

    val finalModifier = if (tag.isNotEmpty()) baseModifier.testTag(tag) else baseModifier

    Box(
        modifier = finalModifier,
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
