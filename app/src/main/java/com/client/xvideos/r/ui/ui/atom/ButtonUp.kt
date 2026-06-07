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

@Composable
fun ButtonUp(width : Dp = 32.dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .width(width)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0x80757575), RoundedCornerShape(8.dp))
            .background(Theme.tabLevel0)
            .clickable(
                onClick = onClick), contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.ArrowUpward,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier
        )
    }
}

@Composable
fun ButtonUpCircle(size : Dp = 46.dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .border(1.dp, Color(0x80757575), CircleShape)
            .background(Theme.tabLevel0)
            .clickable(
                onClick = onClick), contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.ArrowUpward,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier
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


