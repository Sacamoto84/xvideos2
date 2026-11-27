package com.client.xvideos.common.videoplayer.local.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip

@Composable
internal fun PlayerControlIcon(
    icon: ImageVector,
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HintTooltip(text = hint) {
        Icon(
            imageVector = icon,
            contentDescription = hint,
            tint = Color.White,
            modifier = modifier.size(28.dp).clickable(onClick = onClick)
        )
    }
}

@Preview
@Composable
private fun PlayerControlIconPreview() {
    PlayerControlIcon(
        icon = Icons.Filled.PlayArrow,
        hint = "Воспроизвести",
        onClick = {},
    )
}
