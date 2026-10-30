package com.client.xvideos.l.ui.screens.screenAlbumList.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.HintTooltip

@Composable
fun AlbumPageNavButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HintTooltip(text = contentDescription, modifier = modifier.fillMaxHeight()) {
        Box(
            // Ширину задаёт вызывающий (weight) на обёртке подсказки — заливаем её целиком.
            modifier = Modifier
                .fillMaxSize()
                .background(Theme.L.red)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                tint = Color.White,
                contentDescription = contentDescription
            )
        }
    }
}

@Preview
@Composable
private fun AlbumPageNavButtonPreview() {
    AlbumPageNavButton(
        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        contentDescription = "Предыдущая страница",
        onClick = {}
    )
}
