package com.client.xvideos.xlive.ui.record.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.xlive.ui.XLiveColors

/** Серая плашка шапки записи вместо красной «В эфире»: «ЗАПИСЬ · 26 ОКТ 21:40». */
@Composable
fun XLiveRecordBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = XLiveColors.PillContent,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(XLiveColors.Pill, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Preview
@Composable
private fun XLiveRecordBadgePreview() {
    XLiveRecordBadge(text = "ЗАПИСЬ · 26 ОКТ 21:40")
}
