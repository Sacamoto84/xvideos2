package com.client.xvideos.xlive.ui.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

/**
 * Сообщение по центру экрана XLive с кнопкой повтора.
 *
 * @param text что случилось с потоком.
 * @param onRetry повторить запрос комнаты.
 */
@Composable
fun XLiveMessage(
    text: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = text, color = Color.White, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) {
            Text(text = "Повторить")
        }
    }
}

@Preview
@Composable
private fun XLiveMessagePreview() {
    XLiveMessage(text = "Комната не в открытом эфире: offline", onRetry = {})
}
