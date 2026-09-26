package com.client.xvideos.r.ui.ui.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.feature.r.R

private const val INDEX_SINGLE = 1
private const val INDEX_DOUBLE = 2

@Preview
@Composable
fun DefaultPreview() {
    Selector(INDEX_SINGLE, onSelect = {})
}

@Composable
fun Selector(
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    val onSelect1 = remember(onSelect) { { onSelect(INDEX_SINGLE) } }
    val onSelect2 = remember(onSelect) { { onSelect(INDEX_DOUBLE) } }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Theme.R.colorBorderGray, RoundedCornerShape(8.dp))
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

@Composable
private fun SelectorButton(
    @DrawableRes iconRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) Theme.R.colorBorderSelect else Theme.background
    val tint = if (isSelected) Color.White else Theme.R.colorTextGray

    Box(
        modifier = modifier
            .size(46.dp)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
