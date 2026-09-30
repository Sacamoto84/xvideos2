package com.client.xvideos.x.screens.subscriptions.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Экран пустого состояния для подписок.
 */
@Composable
fun SubscriptionsEmptyState(
    isModel: Boolean,
    topCutout: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(top = topCutout)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = if (isModel) Icons.Outlined.Person else Icons.Outlined.Tv,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (isModel) "У вас нет подписок на актрис" else "У вас нет подписок на каналы",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Перейдите на страницу ${if (isModel) "актрисы" else "канала"} и нажмите кнопку «Подписаться»",
                color = Color.Gray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF16161C)
@Composable
private fun SubscriptionsEmptyStatePreview() {
    SubscriptionsEmptyState(
        isModel = true,
        topCutout = 0.dp
    )
}
