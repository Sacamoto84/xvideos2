package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PORNOSTAR_TAG_SHAPE = RoundedCornerShape(6.dp)
private val COUNT_BADGE_SHAPE = RoundedCornerShape(4.dp)
private val COUNT_BADGE_BG = Color(0x33000000)

/**
 * ## Отображение текста канала и порноактрисы и показ количества подписок на них
 */
@Composable
fun ScreenItemTagsModelPornostars(
    text: String,
    color: Color,
    count: String,
    onClick: (() -> Unit)? = null
) {
    val baseModifier = Modifier
        .padding(horizontal = 3.dp, vertical = 2.dp)
        .height(28.dp)
        .clip(PORNOSTAR_TAG_SHAPE)
        .background(color)
    val rowModifier = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            modifier = Modifier.padding(start = 8.dp, end = if (count.isNotBlank()) 4.dp else 8.dp),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        if (count.isNotBlank()) {
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .height(20.dp)
                    .clip(COUNT_BADGE_SHAPE)
                    .background(COUNT_BADGE_BG)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    count,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

