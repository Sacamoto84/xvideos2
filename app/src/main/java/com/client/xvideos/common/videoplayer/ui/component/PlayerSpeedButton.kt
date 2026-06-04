package com.client.xvideos.common.videoplayer.ui.component

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Preview
@Composable
fun PlayerSpeedButtonPreview() {
    PlayerSpeedButton(
        title =  "String",
        size = 48.dp,
        backgroundColor= Color.White,
        titleColor = Color.Gray,
        onClick= {}
    )
}



@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun PlayerSpeedButton(
    title: String,
    size: Dp,
    backgroundColor: Color,
    titleColor: Color,
    onClick: () -> Unit
) {
    // Box with constraints to limit size
    BoxWithConstraints(
        modifier = Modifier
            .height(size)
            .aspectRatio(2.5f)
            .clip(RectangleShape)
            .background(backgroundColor, shape = RoundedCornerShape(6.dp))
            .clickable { onClick() }, // Clickable area for changing playback speed
        contentAlignment = Alignment.Center
    ) {

        // Calculate maximum font size based on height
        val maxFontSize = (maxHeight.value / 2.25).sp

        // Display the playback speed title
        Text(
            text = title,
            color = titleColor,
            style = TextStyle(
                fontSize = maxFontSize,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}