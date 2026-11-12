package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.n.model.NTag

/**
 * Чип тега манги с цветовым кодированием категории.
 */
@Composable
fun NGalleryTagChip(
    tag: NTag,
    onClick: (NTag) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = remember(tag.type) {
        when (tag.type.lowercase()) {
            "artist" -> Color(0xFFED2553)
            "character" -> Color(0xFF1E88E5)
            "parody" -> Color(0xFF43A047)
            "language" -> Color(0xFFFB8C00)
            "group" -> Color(0xFF8E24AA)
            else -> Color(0xFF424242)
        }
    }

    val chipShape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .clip(chipShape)
            .background(categoryColor.copy(alpha = 0.25f))
            .clickable { onClick(tag) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tag.name,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        if (tag.count > 0) {
            Text(
                text = " ${formatCount(tag.count)}",
                color = categoryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp)
            )
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}K"
    else -> count.toString()
}

@Preview
@Composable
private fun NGalleryTagChipPreview() {
    NGalleryTagChip(
        tag = NTag(id = 1L, type = "artist", name = "shindol", count = 15400),
        onClick = {}
    )
}
