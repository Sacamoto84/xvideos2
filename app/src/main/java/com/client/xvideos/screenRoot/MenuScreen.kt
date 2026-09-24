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

private val MENU_WINDOW_INSETS = WindowInsets(0, 0, 0, 0)
private val MENU_BACKGROUND_COLOR = Color(0xFF353535)
private val MENU_BUTTON_SHAPE = RoundedCornerShape(16.dp)
private val MENU_BUTTON_BORDER_COLOR = Color(0xFF565656)
private val MENU_BUTTON_BG_COLOR = Color(0xFF212121)

private val TOP_BAR_BUTTON_SIZE = 48.dp
private val SETTINGS_ICON_SIZE = 32.dp
private val HAPTIC_ICON_SIZE = 30.dp
private val P2P_ICON_SIZE = 28.dp

private val BUTTON_BORDER_WIDTH = 2.dp
private val BUTTON_PADDING = 16.dp
private val BUTTON_IMAGE_HEIGHT = 80.dp

private const val CD_SETTINGS = "Настройки"
private const val CD_HAPTIC = "Haptic demo"
private const val CD_P2P = "Приём P2P"
private const val TAG_BUTTON_L = "buttonL"

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
            contentWindowInsets = MENU_WINDOW_INSETS,
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top)),
                    contentAlignment = Alignment.TopStart
                ) {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.size(TOP_BAR_BUTTON_SIZE)) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = CD_SETTINGS,
                            tint = Color.White,
                            modifier = Modifier.size(SETTINGS_ICON_SIZE))
                    }
                    // Демо-экран виброоткликов (HapticFeedbackType) для тестов
                    IconButton(
                        onClick = onOpenHapticDemo,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(TOP_BAR_BUTTON_SIZE)
                    ) {
                        Icon(
                            Icons.Default.Vibration,
                            contentDescription = CD_HAPTIC,
                            tint = Color.White,
                            modifier = Modifier.size(HAPTIC_ICON_SIZE))
                    }

                    // Приём item по P2P (Nearby)
                    IconButton(
                        onClick = onOpenP2pReceive,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(TOP_BAR_BUTTON_SIZE)
                    ) {
                        Icon(
                            Icons.Default.Wifi,
                            contentDescription = CD_P2P,
                            tint = Color.White,
                            modifier = Modifier.size(P2P_ICON_SIZE))
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MENU_BACKGROUND_COLOR)
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {

                ButtonSelect(R.drawable.icon_xvideos_white, onClick = onOpenX)

                ButtonSelect(R.drawable.icon_luscious, TAG_BUTTON_L, onClick = onOpenL)
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
        .padding(BUTTON_PADDING)
        .fillMaxWidth()
        .clip(MENU_BUTTON_SHAPE)
        .border(BUTTON_BORDER_WIDTH, MENU_BUTTON_BORDER_COLOR, MENU_BUTTON_SHAPE)
        .background(MENU_BUTTON_BG_COLOR)
        .clickable(onClick = onClick)
        .padding(vertical = BUTTON_PADDING)

    val finalModifier = if (tag.isNotEmpty()) baseModifier.testTag(tag) else baseModifier

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painterResource(iconId),
            contentDescription = null,
            modifier = Modifier.height(BUTTON_IMAGE_HEIGHT),
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
