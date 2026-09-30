package com.client.xvideos.x.screens.saved.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

@Composable
fun SavedHeader(
    modifier: Modifier = Modifier,
    topCutout: Dp = 0.dp,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topCutout)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Theme.L.grey6),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Сохранённое",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
        }
        HorizontalDivider(color = Color(0xFF9E9E9E))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141414)
@Composable
private fun SavedHeaderPreview() {
    SavedHeader(topCutout = 0.dp)
}
