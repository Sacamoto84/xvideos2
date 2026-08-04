package com.client.xvideos.common.traficStatistic

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.App
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.ui.theme.XvideosTheme
import kotlin.math.roundToInt

@Composable
fun AppNetworkSpeedMonitorLite() {

    //return

    val context = LocalContext.current
    val application = context.applicationContext as App
    val trafficData by application.networkTrafficMonitor.trafficFlow.collectAsStateWithLifecycle()

//    val rawProgress by remember{
//        derivedStateOf {
//            CoilProgressManager.progressMap.filter { !it.value.done }.size
//        }
//    }
//
////    // Debounce: обновляем UI не чаще 100–200 мс
//    val progress by produceState(rawProgress) {
//        while (true) {
//            value = rawProgress
//            delay(200)
//        }
//    }
//
    AppNetworkSpeedMonitorLiteContent(
        downloadSpeed = trafficData.downloadSpeed,
        sessionDownloaded = trafficData.sessionDownloaded
    )
}

fun formatSpeed(bytesPerSecond: Long): String {
    return when {
        bytesPerSecond < 0 -> "0 Б/c"
        bytesPerSecond < 1024 -> "$bytesPerSecond Б/с"
        bytesPerSecond < 1024 * 1024 -> "${(bytesPerSecond / 1024.0).roundToInt()} КБ/C"
        bytesPerSecond < 1024 * 1024 * 1024 -> "${(bytesPerSecond / (1024.0 * 1024.0) * 10).roundToInt() / 10.0} МБ/c"
        else -> "${(bytesPerSecond / (1024.0 * 1024.0 * 1024.0) * 100).roundToInt() / 100.0} GBs"
    }
}

private val style = TextStyle(
    color = Color.Black,
    fontSize = 8.sp,
    fontWeight = FontWeight.Medium,
    fontFamily = Theme.L.fontFamilyKarla,
    lineHeight = 8.sp
)

private val styleW = style.copy(color = Color.White )

@Composable
fun AppNetworkSpeedMonitorLiteContent(
    downloadSpeed: Long,
    sessionDownloaded: Long
) {
    val formattedSpeed = remember(downloadSpeed) { formatSpeed(downloadSpeed) }
    val formattedBytes = remember(sessionDownloaded) { formatBytes(sessionDownloaded) }

    Row( modifier = Modifier.padding(end = 16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.End )
    {
        Box() {
            Text( formattedSpeed, style = style, modifier = Modifier.offset(0.5.dp, 0.5.dp))
            Text( formattedSpeed, style = styleW )
        }
        Box {
            Text( " / $formattedBytes", style = style, modifier = Modifier.offset(0.5.dp, 0.5.dp))
            Text( " / $formattedBytes", style = styleW )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF303030)
@Composable
fun AppNetworkSpeedMonitorLitePreview() {
    XvideosTheme {
        AppNetworkSpeedMonitorLiteContent(
            downloadSpeed = 1024 * 1024 * 2, // 2 MB/s
            sessionDownloaded = 1024 * 1024 * 150 // 150 MB
        )
    }
}
