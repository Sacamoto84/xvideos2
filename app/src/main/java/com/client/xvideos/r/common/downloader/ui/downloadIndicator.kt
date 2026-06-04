package com.redgifs.common.downloader.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.core.HorizontalSeparator

@Composable
fun DownloadIndicator(percentDownload : Float) {
    //val percentDownload = Downloader.percent.collectAsStateWithLifecycle().value
    //Индикатор загрузки
    when(percentDownload) {
        in  0f..1f -> LinearProgressIndicator(progress = percentDownload,  modifier = Modifier.fillMaxWidth().height(2.dp), color = Color.Green)
        -2f -> HorizontalSeparator(Color.Transparent, thickness = 2.dp)
        -3f -> Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Red))
    }
}