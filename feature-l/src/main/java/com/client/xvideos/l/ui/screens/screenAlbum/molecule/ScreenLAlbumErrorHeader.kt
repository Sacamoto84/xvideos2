package com.client.xvideos.l.ui.screens.screenAlbum.molecule

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

@Composable
fun ScreenLAlbumErrorHeader(
    loadError: String,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Не удалось загрузить данные альбома",
            color = Theme.L.textColor,
            style = Theme.L.Type.rowTitle
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = loadError,
            color = Color.Gray,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Theme.L.red)
            ) {
                Text("Повторить", color = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Theme.tabLevel1)
            ) {
                Text("Назад", color = Theme.L.textColor)
            }
        }
    }
}

@Preview
@Composable
private fun ScreenLAlbumErrorHeaderPreview() {
    ScreenLAlbumErrorHeader(
        loadError = "Ошибка подключения к сети",
        onRetry = {},
        onBack = {}
    )
}
