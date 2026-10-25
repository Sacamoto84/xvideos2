package com.client.xvideos.xlive.ui.root.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.xlive.ui.XLiveColors

/**
 * Подвал ленты: следующая страница не загрузилась, «Повторить» запрашивает её снова.
 *
 * @param message что не получилось.
 * @param onRetry повтор догрузки.
 */
@Composable
fun XLiveLoadMoreError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = XLiveColors.PillContent, fontSize = 13.sp, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) {
            Text(text = "Повторить", color = XLiveColors.Link)
        }
    }
}

@Preview
@Composable
private fun XLiveLoadMoreErrorPreview() {
    XLiveLoadMoreError(message = "Не удалось загрузить ещё", onRetry = {})
}
