package com.client.xvideos.common.videoplayer.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.videoplayer.model.PlayerSpeed

/**
 * Текстовый бейдж текущей скорости воспроизведения по умолчанию.
 */
@Composable
fun DefaultSpeedBadge(
    speed: PlayerSpeed,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = speed.displayName,
        color = Color.White,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Preview
@Composable
private fun DefaultSpeedBadgePreview() {
    DefaultSpeedBadge(speed = PlayerSpeed.DEFAULT, onClick = {})
}
