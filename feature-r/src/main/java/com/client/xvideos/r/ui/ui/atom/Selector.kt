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

@Preview
@Composable
fun DefaultPreview() {
    Selector(INDEX_SINGLE, onSelect = {})
}

@Composable
fun Selector(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val onSelect1 = remember(onSelect) { { onSelect(INDEX_SINGLE) } }
    val onSelect2 = remember(onSelect) { { onSelect(INDEX_DOUBLE) } }

    val bg2 = if (selectedIndex == INDEX_DOUBLE) Theme.R.colorBorderSelect else Theme.background
    val tint2 = if (selectedIndex == INDEX_DOUBLE) Color.White else Theme.R.colorTextGray
    val bg1 = if (selectedIndex == INDEX_SINGLE) Theme.R.colorBorderSelect else Theme.background
    val tint1 = if (selectedIndex == INDEX_SINGLE) Color.White else Theme.R.colorTextGray

    Row(
        modifier = Modifier
            .clip(SELECTOR_SHAPE)
            .border(SELECTOR_BORDER_WIDTH, Theme.R.colorBorderGray, SELECTOR_SHAPE)
    ) {
        Box(
            modifier = Modifier
                .size(SELECTOR_BUTTON_SIZE)
                .background(bg2)
                .clickable(onClick = onSelect2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.select_2),
                contentDescription = null,
                tint = tint2,
                modifier = Modifier.size(SELECTOR_ICON_SIZE)
            )
        }

        Box(
            modifier = Modifier
                .width(SELECTOR_DIVIDER_WIDTH)
                .height(SELECTOR_BUTTON_SIZE)
                .background(Theme.R.colorBorderGray)
        )

        Box(
            modifier = Modifier
                .size(SELECTOR_BUTTON_SIZE)
                .background(bg1)
                .clickable(onClick = onSelect1),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.select_1),
                contentDescription = null,
                tint = tint1,
                modifier = Modifier.size(SELECTOR_ICON_SIZE)
            )
        }
    }
}
