package com.client.xvideos.x.screens.channel.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InfoFieldItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = Color(0xFF888888),
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = Color(0xFFE0E0E0),
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
private fun InfoFieldItemPreview() {
    InfoFieldItem(label = "Возраст", value = "24 года")
}
