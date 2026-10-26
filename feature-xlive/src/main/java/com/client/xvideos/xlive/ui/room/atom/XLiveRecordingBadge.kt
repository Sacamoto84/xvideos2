package com.client.xvideos.xlive.ui.room.atom

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.xlive.model.recordingElapsedLabel
import com.client.xvideos.xlive.ui.XLiveColors
import kotlinx.coroutines.delay

private const val TICK_MS = 1_000L

/**
 * Плашка на видео, пока идёт запись: «● 03:12». Время тикает раз в секунду.
 *
 * @param startedAtElapsedMs начало записи по `SystemClock.elapsedRealtime()`.
 */
@Composable
fun XLiveRecordingBadge(
    startedAtElapsedMs: Long,
    modifier: Modifier = Modifier,
) {
    val elapsed by produceState(0L, startedAtElapsedMs) {
        while (true) {
            value = SystemClock.elapsedRealtime() - startedAtElapsedMs
            delay(TICK_MS)
        }
    }
    Text(
        text = "● ${recordingElapsedLabel(elapsed)}",
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(XLiveColors.Accent, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Preview
@Composable
private fun XLiveRecordingBadgePreview() {
    XLiveRecordingBadge(startedAtElapsedMs = 0L)
}
