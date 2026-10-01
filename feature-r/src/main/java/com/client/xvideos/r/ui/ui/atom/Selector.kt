package com.client.xvideos.r.ui.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.feature.r.R

private const val INDEX_SINGLE = 1
private const val INDEX_DOUBLE = 2

@Composable
fun Selector(
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    val onSelect1 = remember(onSelect) { { onSelect(INDEX_SINGLE) } }
    val onSelect2 = remember(onSelect) { { onSelect(INDEX_DOUBLE) } }
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .clip(shape)
            .border(1.dp, Theme.R.colorBorderGray, shape)
    ) {
        SelectorButton(
            iconRes = R.drawable.select_2,
            isSelected = selectedIndex == INDEX_DOUBLE,
            onClick = onSelect2
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(46.dp)
                .background(Theme.R.colorBorderGray)
        )

        SelectorButton(
            iconRes = R.drawable.select_1,
            isSelected = selectedIndex == INDEX_SINGLE,
            onClick = onSelect1
        )
    }
}

@Preview
@Composable
private fun SelectorPreview() {
    Selector(INDEX_SINGLE, onSelect = {})
}
