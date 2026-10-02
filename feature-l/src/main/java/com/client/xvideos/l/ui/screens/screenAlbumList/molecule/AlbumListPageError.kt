package com.client.xvideos.l.ui.screens.screenAlbumList.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

/**
 * Страница списка альбомов не загрузилась: причина и «Повторить» вместо
 * пустой сетки.
 *
 * @param message Текст ошибки для пользователя.
 * @param onRetry Повторная загрузка этой страницы.
 */
@Composable
fun AlbumListPageError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = Theme.L.textColor,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Theme.L.b0)
            ) {
                Text("Повторить", color = Color.White)
            }
        }
    }
}

@Preview
@Composable
private fun AlbumListPageErrorPreview() {
    AlbumListPageError(message = "Сервер L недоступен (HTTP 500)", onRetry = {})
}
