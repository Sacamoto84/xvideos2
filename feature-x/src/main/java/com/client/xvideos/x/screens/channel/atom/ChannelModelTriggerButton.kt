package com.client.xvideos.x.screens.channel.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.model.ChannelModelFilterItem

@Composable
fun ChannelModelTriggerButton(
    selectedModel: ChannelModelFilterItem?,
    isExpanded: Boolean,
    placeholderText: String,
    onExpandedChange: (Boolean) -> Unit,
    onSelectModel: (ChannelModelFilterItem?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF16161A))
            .border(
                1.dp,
                if (isExpanded || selectedModel != null) Color(0xFFDE2600) else Color(0xFF333338),
                RoundedCornerShape(6.dp)
            )
            .clickable { onExpandedChange(!isExpanded) }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = if (selectedModel != null) Color(0xFFDE2600) else Color(0xFF888888),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = selectedModel?.displayName ?: placeholderText,
            fontSize = 12.sp,
            color = if (selectedModel != null) Color.White else Color(0xFF8E8E93),
            fontWeight = if (selectedModel != null) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp)
        )

        Spacer(modifier = Modifier.width(4.dp))

        if (selectedModel != null) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Сбросить фильтр",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onSelectModel(null) }
            )
        } else {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF888888),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ChannelModelTriggerButtonPreview() {
    ChannelModelTriggerButton(
        selectedModel = null,
        isExpanded = false,
        placeholderText = "Поиск модели...",
        onExpandedChange = {},
        onSelectModel = {}
    )
}
