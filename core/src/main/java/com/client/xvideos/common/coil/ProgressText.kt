package com.client.xvideos.common.coil

import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.formatBytes

@Composable
internal fun ProgressText(
    bytesRead: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier,
    visibleByte: Boolean = true,
) {
    if (bytesRead > 1000) {
        val text = if (totalBytes > 0) {
            if (visibleByte) "${formatBytes(bytesRead)} / ${formatBytes(totalBytes)}" else formatBytes(
                totalBytes
            )
        } else {
            formatBytes(bytesRead)
        }

        Text(
            text,
            color = Color.White,
            modifier = modifier.offset((-1).dp, 7.dp),
            fontFamily = Theme.L.fontFamilyKarla,
            fontSize = 9.sp
        )
    }
}

@Preview
@Composable
private fun ProgressTextPreview() {
    ProgressText(bytesRead = 250_000, totalBytes = 1_000_000)
}
