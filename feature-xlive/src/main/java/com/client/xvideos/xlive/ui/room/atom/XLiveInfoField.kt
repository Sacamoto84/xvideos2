package com.client.xvideos.xlive.ui.room.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.xlive.ui.XLiveColors

/** Поле анкеты «метка: значение» в карточке «О модели», как у канала X. */
@Composable
fun XLiveInfoField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            color = XLiveColors.Label,
            fontWeight = FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = XLiveColors.Value,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Composable
private fun XLiveInfoFieldPreview() {
    XLiveInfoField(label = "Возраст", value = "24 года")
}
