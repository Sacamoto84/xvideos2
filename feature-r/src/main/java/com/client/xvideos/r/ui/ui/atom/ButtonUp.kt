package com.client.xvideos.r.ui.ui.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BUTTON_UP_BORDER_COLOR = Color(0x80757575)
private val BUTTON_UP_BORDER_WIDTH = 1.dp
private val BUTTON_UP_CORNER_RADIUS = 8.dp
private val BUTTON_UP_ROUNDED_SHAPE = RoundedCornerShape(BUTTON_UP_CORNER_RADIUS)
private val BUTTON_UP_HEIGHT = 46.dp
private val DEFAULT_BUTTON_UP_WIDTH = 32.dp
private val DEFAULT_BUTTON_UP_CIRCLE_SIZE = 46.dp
private val BUTTON_UP_ICON_TINT = Color.LightGray

@Composable
fun ButtonUp(
    width: Dp = DEFAULT_BUTTON_UP_WIDTH,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(BUTTON_UP_HEIGHT)
            .width(width)
            .clip(BUTTON_UP_ROUNDED_SHAPE)
            .border(BUTTON_UP_BORDER_WIDTH, BUTTON_UP_BORDER_COLOR, BUTTON_UP_ROUNDED_SHAPE)
            .background(Theme.tabLevel0)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.ArrowUpward,
            contentDescription = null,
            tint = BUTTON_UP_ICON_TINT
        )
    }
}

@Composable
fun ButtonUpCircle(
    size: Dp = DEFAULT_BUTTON_UP_CIRCLE_SIZE,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(BUTTON_UP_BORDER_WIDTH, BUTTON_UP_BORDER_COLOR, CircleShape)
            .background(Theme.tabLevel0)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.ArrowUpward,
            contentDescription = null,
            tint = BUTTON_UP_ICON_TINT
        )
    }
}

@Preview
@Composable
fun ButtonUpPreview() {
    ButtonUp(onClick = {})
}

@Preview
@Composable
fun ButtonUpCirclePreview() {
    ButtonUpCircle(onClick = {})
}
