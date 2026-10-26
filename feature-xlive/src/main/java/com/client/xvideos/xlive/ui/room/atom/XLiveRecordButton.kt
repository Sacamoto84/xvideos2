package com.client.xvideos.xlive.ui.room.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
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
 * Кнопка записи эфира в шапке комнаты. Не пишет — красная точка; пишет —
 * красная кнопка с квадратом «стоп»; пока запись склеивается — крутилка, и
 * кнопка не нажимается.
 *
 * @param recording запись идёт.
 * @param finishing запись остановлена и склеивается.
 */
@Composable
fun XLiveRecordButton(
    recording: Boolean,
    finishing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = when {
        finishing -> "Запись сохраняется"
        recording -> "Остановить запись"
        else -> "Записать эфир"
    }
    HintTooltip(text = hint, modifier = modifier) {
        IconButton(
            onClick = onClick,
            enabled = !finishing,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (recording) XLiveColors.Accent else XLiveColors.Pill)
        ) {
            if (finishing) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(
                    imageVector = if (recording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                    contentDescription = hint,
                    tint = if (recording) Color.White else XLiveColors.Accent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun XLiveRecordButtonPreview() {
    XLiveRecordButton(recording = true, finishing = false, onClick = {})
}
