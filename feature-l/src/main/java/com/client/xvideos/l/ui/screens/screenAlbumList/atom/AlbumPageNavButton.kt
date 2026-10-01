package com.client.xvideos.l.ui.screens.screenAlbumList.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
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

@Composable
fun AlbumPageNavButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navButtonBase = Modifier
        .fillMaxHeight()
        .background(Theme.L.red)
    val baseModifier = if (modifier == Modifier) navButtonBase else modifier.then(navButtonBase)
    Box(
        modifier = baseModifier.clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            tint = Color.White,
            contentDescription = contentDescription
        )
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
