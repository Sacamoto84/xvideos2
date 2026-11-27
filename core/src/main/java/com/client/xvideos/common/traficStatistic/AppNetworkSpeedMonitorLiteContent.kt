package com.client.xvideos.common.traficStatistic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.edge.avoidCameraAndCorner
import com.client.xvideos.common.util.formatBytes
import kotlin.math.roundToInt

private fun formatTrafficSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 0 -> "0 Б/с"
        bytesPerSecond < 1024 -> "$bytesPerSecond Б/с"
        bytesPerSecond < 1024 * 1024 -> "${(bytesPerSecond / 1024.0).roundToInt()} КБ/с"
        bytesPerSecond < 1024 * 1024 * 1024 -> "${(bytesPerSecond / (1024.0 * 1024.0) * 10).roundToInt() / 10.0} МБ/с"
        else -> "${(bytesPerSecond / (1024.0 * 1024.0 * 1024.0) * 100).roundToInt() / 100.0} ГБ/с"
    }
}

private val style = TextStyle(
    color = Color.Black,
    fontSize = 8.sp,
    fontWeight = FontWeight.Medium,
    fontFamily = Theme.L.fontFamilyKarla,
    lineHeight = 8.sp
)

private val styleW = style.copy(color = Color.White)

@Composable
fun AppNetworkSpeedMonitorLiteContent(
    downloadSpeed: Long,
    sessionDownloaded: Long
) {
    val formattedSpeed = remember(downloadSpeed) { formatTrafficSpeed(downloadSpeed) }
    val formattedBytes = remember(sessionDownloaded) { formatBytes(sessionDownloaded) }

    Row(modifier = Modifier.padding(end = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Row(modifier = Modifier.avoidCameraAndCorner()) {
            Box {
                Text(formattedSpeed, style = style, modifier = Modifier.offset(0.5.dp, 0.5.dp))
                Text(formattedSpeed, style = styleW)
            }
            Box {
                Text(" / $formattedBytes", style = style, modifier = Modifier.offset(0.5.dp, 0.5.dp))
                Text(" / $formattedBytes", style = styleW)
            }
        }
    }
}

@Preview
@Composable
private fun AppNetworkSpeedMonitorLitePreview() {
    AppNetworkSpeedMonitorLiteContent(
        downloadSpeed = 1024 * 1024 * 2L,
        sessionDownloaded = 1024 * 1024 * 150L
    )
}
