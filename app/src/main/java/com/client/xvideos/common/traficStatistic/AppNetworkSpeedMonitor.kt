package com.client.xvideos.common.traficStatistic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.App
import com.client.xvideos.common.util.formatBytes
import com.client.xvideos.l.theme.ThemeL

@Composable
fun AppNetworkSpeedMonitor() {
    val context = LocalContext.current
    val application = context.applicationContext as? App
    val trafficData = application?.networkTrafficMonitor?.trafficFlow?.collectAsStateWithLifecycle()?.value ?: TrafficData()
    AppNetworkSpeedMonitorContent(trafficData)
}

@Composable
fun AppNetworkSpeedMonitorContent(trafficData: TrafficData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Заголовок
        Text(
            text = "Трафик приложения",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = ThemeL.textColor,
            fontFamily = ThemeL.fontFamilyKarla,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Column(modifier = Modifier.padding(0.dp))
        {
            SpeedRow(
                label = "📊 За текущую сессию:",
                value = formatBytes(trafficData.sessionDownloaded + trafficData.sessionUploaded)
            )
        }

        // Карточка общих объемов
        Column(modifier = Modifier.padding(0.dp))
        {
            SpeedRow(
                label = "📊 За все время:",
                value = formatBytes(trafficData.totalDownloaded + trafficData.totalUploaded)
            )
        }
    }
}

@Preview
@Composable
fun AppNetworkSpeedMonitorPreview() {
    AppNetworkSpeedMonitorContent(TrafficData(sessionDownloaded = 1024, totalDownloaded = 2048))
}

@Composable
fun SpeedRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = ThemeL.textColor, fontFamily = ThemeL.fontFamilyKarla
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = ThemeL.textColor, fontFamily = ThemeL.fontFamilyKarla
        )
    }
}

@Preview
@Composable
fun SpeedRowPreview() {
    SpeedRow(label = "Download:", value = "10.5 MBs")
}
