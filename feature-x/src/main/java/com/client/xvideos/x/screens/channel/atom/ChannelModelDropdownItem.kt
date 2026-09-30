package com.client.xvideos.x.screens.channel.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelModelFilterItem

@Composable
fun ChannelModelDropdownItem(
    model: ChannelModelFilterItem,
    isSelected: Boolean,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconText = when {
        model.isWoman -> "♀"
        model.isMan -> "♂"
        else -> "📺"
    }
    val iconColor = when {
        model.isWoman -> Color(0xFFE91E63)
        model.isMan -> Color(0xFF2196F3)
        else -> Color(0xFF1E88E5)
    }

    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = model.displayName,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color(0xFFDE2600) else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (model.countText.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${model.countText} видео)",
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
            }
        },
        leadingIcon = {
            Text(
                text = iconText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
        },
        onClick = {
            onSelectModel(model)
            onExpandedChange(false)
        },
        modifier = if (isSelected) modifier.background(Color(0xFF281418)) else modifier
    )
}

@Preview
@Composable
private fun ChannelModelDropdownItemPreview() {
    ChannelModelDropdownItem(
        model = ChannelModelFilterItem(
            idUser = 1L,
            displayName = "Jane Doe",
            gender = "Woman",
            fNbVideos = "15"
        ),
        isSelected = true,
        onSelectModel = {},
        onExpandedChange = {}
    )
}
