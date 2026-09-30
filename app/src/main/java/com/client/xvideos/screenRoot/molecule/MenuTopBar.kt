package com.client.xvideos.screenRoot.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MenuTopBar(
    onOpenSettings: () -> Unit,
    onOpenHapticDemo: () -> Unit,
    onOpenP2pReceive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top)),
        contentAlignment = Alignment.TopStart
    ) {
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Настройки",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
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
                modifier = Modifier.size(30.dp)
            )
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
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Preview
@Composable
private fun MenuTopBarPreview() {
    MenuTopBar(
        onOpenSettings = {},
        onOpenHapticDemo = {},
        onOpenP2pReceive = {}
    )
}
