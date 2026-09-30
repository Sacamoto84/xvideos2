package com.client.xvideos.r.ui.explorer.tab.saved.tab.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toPrettyCount

@Composable
fun CreatorMetric(
    label: String,
    value: Long,
    modifier: Modifier = Modifier
) {
    val prettyValue = remember(value) { value.toPrettyCount() }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF242424))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            prettyValue,
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = Theme.R.fontFamilyPopinsRegular,
            maxLines = 1
        )
        Text(
            label,
            color = Color(0xFF9E9DA9),
            fontSize = 9.sp,
            fontFamily = Theme.R.fontFamilyDMsanss,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
private fun CreatorMetricPreview() {
    CreatorMetric(
        label = "Подписчики",
        value = 12500L
    )
}
