package com.client.xvideos.l.ui.element.lazyRowPictureDetails.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme

@Composable
fun AnimatedVideoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Theme.L.grey6),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun AnimatedVideoPlaceholderPreview() {
    AnimatedVideoPlaceholder(
        modifier = Modifier
            .width(200.dp)
            .height(150.dp)
    )
}
