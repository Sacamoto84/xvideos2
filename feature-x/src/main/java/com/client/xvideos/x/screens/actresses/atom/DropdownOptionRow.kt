package com.client.xvideos.x.screens.actresses.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.client.xvideos.x.model.ActressesIndexFilterOption

@Composable
fun DropdownOptionRow(
    option: ActressesIndexFilterOption,
    onSelectOption: (ActressesIndexFilterOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = option.isActive
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF2A1518) else Color.Transparent)
            .clickable { onSelectOption(option) }
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(
            text = option.title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFFDE2600) else Color(0xFFE0E0E0),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFFDE2600),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Preview
@Composable
private fun DropdownOptionRowPreview() {
    DropdownOptionRow(
        option = ActressesIndexFilterOption(
            title = "Россия",
            urlPath = "/from/russia",
            isActive = true
        ),
        onSelectOption = {}
    )
}
