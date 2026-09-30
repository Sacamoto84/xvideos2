package com.client.xvideos.r.ui.profile.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme

@Composable
fun ExpandCollapseButton(
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .size(32.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(1.dp, Theme.R.colorYellow, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.Close else Icons.Default.MoreHoriz,
            contentDescription = if (expanded) "Свернуть теги" else "Развернуть теги",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Preview
@Composable
private fun ExpandCollapseButtonPreview() {
    ExpandCollapseButton(
        expanded = false,
        onClick = {}
    )
}
