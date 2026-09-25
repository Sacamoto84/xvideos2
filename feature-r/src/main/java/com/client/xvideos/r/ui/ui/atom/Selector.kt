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
private val SELECTOR_SHAPE = RoundedCornerShape(8.dp)
private val SELECTOR_BUTTON_SIZE = 46.dp
private val SELECTOR_ICON_SIZE = 24.dp
private val SELECTOR_DIVIDER_WIDTH = 1.dp
private val SELECTOR_BORDER_WIDTH = 1.dp
private val SELECTOR_DIVIDER_MODIFIER = Modifier
    .width(SELECTOR_DIVIDER_WIDTH)
    .height(SELECTOR_BUTTON_SIZE)

private val BOX_ALIGNMENT_CENTER = Alignment.Center
private val COLOR_WHITE = Color.White
private val BUTTON_SIZE_MODIFIER = Modifier.size(SELECTOR_BUTTON_SIZE)
private val ICON_SIZE_MODIFIER = Modifier.size(SELECTOR_ICON_SIZE)

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

    val borderColor = Theme.R.colorBorderGray
    val borderModifier = remember(borderColor) {
        Modifier
            .clip(SELECTOR_SHAPE)
            .border(SELECTOR_BORDER_WIDTH, borderColor, SELECTOR_SHAPE)
    }
    val dividerModifier = remember(borderColor) {
        SELECTOR_DIVIDER_MODIFIER.background(borderColor)
    }
    val rowModifier = if (modifier == Modifier) borderModifier else modifier.then(borderModifier)

    Row(
        modifier = rowModifier
    ) {
        SelectorButton(
            iconRes = R.drawable.select_2,
            isSelected = selectedIndex == INDEX_DOUBLE,
            onClick = onSelect2
        )

        Box(
            modifier = dividerModifier
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
    val tint = if (isSelected) COLOR_WHITE else Theme.R.colorTextGray
    val styledBase = remember(bg) {
        BUTTON_SIZE_MODIFIER.background(bg)
    }
    val boxModifier = if (modifier == Modifier) styledBase else modifier.then(styledBase)

    Box(
        modifier = boxModifier
            .clickable(onClick = onClick),
        contentAlignment = BOX_ALIGNMENT_CENTER
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = ICON_SIZE_MODIFIER
        )
    }
}
