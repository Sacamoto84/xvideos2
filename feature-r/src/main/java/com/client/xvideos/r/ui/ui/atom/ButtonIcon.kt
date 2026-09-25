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
private val BUTTON_ICON_CORNER = 8.dp
private val BUTTON_ICON_SHAPE = RoundedCornerShape(BUTTON_ICON_CORNER)
private val BUTTON_ICON_SIZE = 46.dp
private val BUTTON_ICON_BORDER_WIDTH = 1.dp
private val BUTTON_ICON_TINT = Color.LightGray

private val BUTTON_ICON_BASE_MODIFIER = Modifier
    .size(BUTTON_ICON_SIZE)
    .clip(BUTTON_ICON_SHAPE)
    .border(BUTTON_ICON_BORDER_WIDTH, BUTTON_ICON_BORDER_COLOR, BUTTON_ICON_SHAPE)
    .background(Theme.R.colorCommonBackground)

@Composable
fun ButtonIcon(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(BUTTON_ICON_BASE_MODIFIER)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = BUTTON_ICON_TINT
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
