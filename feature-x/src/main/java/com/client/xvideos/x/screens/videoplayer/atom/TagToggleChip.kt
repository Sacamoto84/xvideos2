package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TagToggleChip(
    text: String,
    isExpanded: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 3.dp, vertical = 2.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xDD444444))
            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
            )
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .rotate(if (isExpanded) 180f else 0f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun TagToggleChipPreview() {
    Row {
        TagToggleChip(
            text = "+5",
            isExpanded = false,
            contentDescription = "Развернуть теги",
            onClick = {},
        )
        TagToggleChip(
            text = "",
            isExpanded = true,
            contentDescription = "Свернуть теги",
            onClick = {},
        )

        TagToggleChip(
            text = "",
            isExpanded = false,
            contentDescription = "Свернуть теги",
            onClick = {},
        )
    }
}
