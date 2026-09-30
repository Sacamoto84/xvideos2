package com.client.xvideos.x.screens.channel.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SubscribeHeaderButton(
    isSubscribed: Boolean,
    isModel: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brandColor = if (isModel) Color(0xFFDE2600) else Color(0xFF1E88E5)
    val bgColor = if (isSubscribed) brandColor.copy(alpha = 0.22f) else Color(0xFF222226)
    val borderColor = if (isSubscribed) brandColor else Color(0xFF444448)
    val textColor = if (isSubscribed) brandColor else Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isSubscribed) Icons.Default.Check else Icons.Default.Add,
            contentDescription = null,
            tint = textColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = if (isSubscribed) "В подписках" else "Подписаться",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Preview
@Composable
private fun SubscribeHeaderButtonPreview() {
    SubscribeHeaderButton(
        isSubscribed = false,
        isModel = true,
        onClick = {}
    )
}
