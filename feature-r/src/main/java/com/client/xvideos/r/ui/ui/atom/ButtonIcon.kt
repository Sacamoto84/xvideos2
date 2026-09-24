package com.client.xvideos.r.ui.ui.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.ui.theme.XvideosTheme

private val BUTTON_ICON_BORDER_COLOR = Color(0x80757575)
private val BUTTON_ICON_SHAPE = RoundedCornerShape(8.dp)
private val BUTTON_ICON_SIZE = 46.dp

@Composable
fun ButtonIcon(imageVector: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(BUTTON_ICON_SIZE)
            .clip(BUTTON_ICON_SHAPE)
            .border(1.dp, BUTTON_ICON_BORDER_COLOR, BUTTON_ICON_SHAPE)
            .background(Theme.R.colorCommonBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = Color.LightGray
        )
    }
}

@Preview
@Composable
private fun ButtonIconPreview() {
    XvideosTheme {
        ButtonIcon(
            imageVector = Icons.Filled.Favorite,
            onClick = {}
        )
    }
}
