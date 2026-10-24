package com.client.xvideos.xlive.ui.room.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.xlive.ui.XLiveColors

/**
 * Сообщение по центру экрана комнаты с кнопкой повтора.
 *
 * @param text что случилось с комнатой.
 * @param onRetry повторить запрос комнаты.
 */
@Composable
fun XLiveMessage(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = text, color = XLiveColors.Value, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) {
            Text(text = "Повторить", color = XLiveColors.Link)
        }
    }
}

@Preview
@Composable
private fun XLiveMessagePreview() {
    XLiveMessage(text = "Комната не в открытом эфире: offline", onRetry = {})
}
