package com.client.xvideos.screenRoot

import com.client.xvideos.R
import com.client.xvideos.HapticDemoScreen
import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.p2p.ui.ScreenP2pReceive
import com.client.xvideos.common.settings.ui.AppSettingsScreen
import com.client.xvideos.l.ui.screens.explorer.L_ScreenExplorer
import com.client.xvideos.r.ui.root.R_Screen_Root
import com.client.xvideos.x.screens.dashboards.ScreenXDashBoards

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
