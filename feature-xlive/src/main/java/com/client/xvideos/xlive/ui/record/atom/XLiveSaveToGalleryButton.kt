package com.client.xvideos.xlive.ui.record.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip
import com.client.xvideos.xlive.ui.XLiveColors

/**
 * Круглая кнопка в шапке записи: сохранить видеозапись в общую галерею устройства.
 */
@Composable
fun XLiveSaveToGalleryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = "Сохранить в галерею"
    HintTooltip(text = hint, modifier = modifier) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(XLiveColors.Pill)
        ) {
            Icon(
                imageVector = Icons.Filled.SaveAlt,
                contentDescription = hint,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview
@Composable
private fun XLiveSaveToGalleryButtonPreview() {
    XLiveSaveToGalleryButton(onClick = {})
}
