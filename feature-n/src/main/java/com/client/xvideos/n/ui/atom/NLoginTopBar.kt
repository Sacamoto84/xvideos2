package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Верхняя панель диалога авторизации с кнопками проверки, перезагрузки и переключения режима.
 */
@Composable
fun NLoginTopBar(
    isManualMode: Boolean,
    onToggleMode: () -> Unit,
    onVerifyClick: () -> Unit,
    onReloadClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2B2B))
    ) {
        if (topInset > 0.dp) {
            Spacer(Modifier.height(topInset))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isManualMode) "Ввод ключа / Cookie" else "Вход в nhentai (Лимиты API)",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (!isManualMode) {
                TextButton(onClick = onVerifyClick) {
                    Text(
                        text = "Я вошёл",
                        color = Color(0xFF4CAF50),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onReloadClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить страницу",
                        tint = Color.White
                    )
                }
            }
            TextButton(onClick = onToggleMode) {
                Text(
                    text = if (isManualMode) "Веб-форма" else "Ключ / Cookie",
                    color = Color(0xFFED2553),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White
                )
            }
        }
    }
}

@Preview
@Composable
private fun NLoginTopBarPreview() {
    NLoginTopBar(
        isManualMode = false,
        onToggleMode = {},
        onVerifyClick = {},
        onReloadClick = {},
        onDismiss = {}
    )
}
