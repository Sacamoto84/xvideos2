package com.client.xvideos.x.screens.actresses.atom

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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

/**
 * Подвал списка: следующая страница не загрузилась, «Повторить» запрашивает её снова.
 *
 * @param message Текст ошибки.
 * @param onRetry Повтор подгрузки.
 */
@Composable
fun LoadMoreErrorFooter(
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
        Text(
            text = message,
            color = Color(0xFFCCCCCC),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDE2600))
        ) {
            Text("Повторить", color = Color.White)
        }
    }
}

@Preview
@Composable
private fun LoadMoreErrorFooterPreview() {
    LoadMoreErrorFooter(message = "Не удалось загрузить следующую страницу", onRetry = {})
}
