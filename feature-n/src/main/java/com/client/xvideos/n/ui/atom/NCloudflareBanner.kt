package com.client.xvideos.n.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Плашка с уведомлением о проверке Cloudflare и кнопкой подтверждения.
 */
@Composable
fun NCloudflareBanner(
    onVerifyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bannerShape = RoundedCornerShape(12.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(bannerShape)
            .border(1.dp, Color(0xFFED2553), bannerShape)
            .background(Color(0xFF261A1D))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Требуется проверка Cloudflare",
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Сайт nhentai.net запросил подтверждение того, что вы человек.",
            color = Color(0xFFCCCCCC),
            fontSize = 13.sp
        )
        Button(
            onClick = onVerifyClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFED2553)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Пройти проверку в браузере",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview
@Composable
private fun NCloudflareBannerPreview() {
    NCloudflareBanner(onVerifyClick = {})
}
