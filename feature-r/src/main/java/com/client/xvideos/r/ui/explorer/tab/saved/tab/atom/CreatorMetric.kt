package com.client.xvideos.r.ui.explorer.tab.saved.tab.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.toPrettyCount
import com.client.xvideos.core.R

private val CREATOR_METRIC_AUTO_SIZE = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 16.sp)

@Composable
fun CreatorMetric(
    label: String,
    value: Long,
    icon: Painter,
    modifier: Modifier = Modifier
) {
    val prettyValue = remember(value) { value.toPrettyCount() }
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label: $value" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.LightGray
        )
        BasicText(
            text = prettyValue,
            style = TextStyle(color = Color.LightGray, fontFamily = Theme.R.fontFamilyDMsanss),
            autoSize = CREATOR_METRIC_AUTO_SIZE,
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
        value = 12500L,
        icon = painterResource(R.drawable.members)
    )
}
