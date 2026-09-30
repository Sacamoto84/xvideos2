package com.client.xvideos.screenRoot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.HapticDemoScreen
import com.client.xvideos.R
import com.client.xvideos.common.p2p.ui.ScreenP2pReceive
import com.client.xvideos.l.ui.screens.explorer.L_ScreenExplorer
import com.client.xvideos.r.ui.root.R_Screen_Root
import com.client.xvideos.screenRoot.atom.MenuButtonSelect
import com.client.xvideos.screenRoot.molecule.MenuTopBar
import com.client.xvideos.screenSettings.AppSettingsScreen
import com.client.xvideos.x.screens.dashboards.ScreenXDashBoards

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

        MenuScreenContent(
            onOpenSettings = onOpenSettings,
            onOpenHapticDemo = onOpenHapticDemo,
            onOpenP2pReceive = onOpenP2pReceive,
            onOpenX = onOpenX,
            onOpenL = onOpenL,
            onOpenR = onOpenR
        )
    }
}

@Composable
fun MenuScreenContent(
    onOpenSettings: () -> Unit,
    onOpenHapticDemo: () -> Unit,
    onOpenP2pReceive: () -> Unit,
    onOpenX: () -> Unit,
    onOpenL: () -> Unit,
    onOpenR: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MenuTopBar(
                onOpenSettings = onOpenSettings,
                onOpenHapticDemo = onOpenHapticDemo,
                onOpenP2pReceive = onOpenP2pReceive
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF353535))
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            MenuButtonSelect(R.drawable.icon_xvideos_white, onClick = onOpenX)
            MenuButtonSelect(R.drawable.icon_luscious, tag = "buttonL", onClick = onOpenL)
            MenuButtonSelect(R.drawable.icon_red, onClick = onOpenR)
        }
    }
}

/**
 * Preview стартового меню для быстрой проверки в Compose Preview.
 */
@Preview(device = "id:pixel_9_pro")
@Composable
private fun MenuPreview() {
    MenuScreenContent(
        onOpenSettings = {},
        onOpenHapticDemo = {},
        onOpenP2pReceive = {},
        onOpenX = {},
        onOpenL = {},
        onOpenR = {}
    )
}
