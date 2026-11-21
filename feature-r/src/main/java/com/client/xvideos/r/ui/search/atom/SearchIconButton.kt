package com.client.xvideos.r.ui.search.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip

@Composable
fun SearchIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    HintTooltip(text = contentDescription, modifier = modifier) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFF757575),
            modifier = Modifier
                .size(38.dp)
                .padding(6.dp)
                .clickable(onClick = onClick)
        )
    }
}

@Preview
@Composable
private fun SearchIconButtonPreview() {
    SearchIconButton(
        icon = Icons.Default.Clear,
        onClick = {},
        contentDescription = "Очистить поле поиска"
    )
}
