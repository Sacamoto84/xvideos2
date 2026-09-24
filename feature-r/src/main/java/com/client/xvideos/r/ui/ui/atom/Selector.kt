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

@Preview
@Composable
fun DefaultPreview() {
    Selector(1, onSelect = {})
}

@Composable
fun Selector(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val selectorShape = remember { RoundedCornerShape(8.dp) }
    val onSelect1 = remember(onSelect) { { onSelect(1) } }
    val onSelect2 = remember(onSelect) { { onSelect(2) } }

    val bg2 = if (selectedIndex == 2) Theme.R.colorBorderSelect else Theme.background
    val tint2 = if (selectedIndex == 2) Color.White else Theme.R.colorTextGray
    val bg1 = if (selectedIndex == 1) Theme.R.colorBorderSelect else Theme.background
    val tint1 = if (selectedIndex == 1) Color.White else Theme.R.colorTextGray

    Row(
        modifier = Modifier
            .clip(selectorShape)
            .border(1.dp, Theme.R.colorBorderGray, selectorShape)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(bg2)
                .clickable(onClick = onSelect2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.select_2),
                contentDescription = null,
                tint = tint2,
                modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(46.dp)
                .background(Theme.R.colorBorderGray)
        )

        Box(
            modifier = Modifier
                .size(46.dp)
                .background(bg1)
                .clickable(onClick = onSelect1),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.select_1),
                contentDescription = null,
                tint = tint1,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}




