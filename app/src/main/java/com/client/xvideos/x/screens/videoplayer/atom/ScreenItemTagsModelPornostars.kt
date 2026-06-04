package com.client.xvideos.screens.videoplayer.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ## Отображение текста канала и порноактрисы и показ количества подписок на них
 */
@Composable
fun ScreenItemTagsModelPornostars(text: String, color: Color, count: String) {

    Row(
        modifier = Modifier
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .height(26.dp)
            .background(color),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text,
            modifier = Modifier
                .padding(start = 2.dp)
                .background(color),
            color = Color.White, fontWeight = FontWeight.Medium
        )


        Box(
            modifier = Modifier
                .padding(start = 2.dp, end = 2.dp)
                .height(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                count,
                modifier = Modifier
                    .fillMaxHeight()
                    .background(Color.White)
                    .padding(horizontal = 2.dp),
                color = Color.Black,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold
            )
        }

    }


}