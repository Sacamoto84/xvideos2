package com.client.xvideos.xlive.ui.saved.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Плашка в углу снимка записи: длительность готовой записи или её состояние
 * («сохраняется», «не завершена»).
 */
@Composable
fun XLiveRecordStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Color(0xCC000000), RoundedCornerShape(3.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

@Preview
@Composable
private fun XLiveRecordStatusBadgePreview() {
    XLiveRecordStatusBadge(text = "1:12:05")
}
