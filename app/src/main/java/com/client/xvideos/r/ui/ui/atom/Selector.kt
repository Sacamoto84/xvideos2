package com.client.xvideos.r.ui.ui.atom

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.R
import com.client.xvideos.r.common.ThemeRed

@Preview
@Composable
fun DefaultPreview() {
    Selector(1, onSelect = {})
}

@Composable
fun Selector(selectedIndex: Int, onSelect: (Int) -> Unit) {

    Row(

        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, ThemeRed.colorBorderGray, RoundedCornerShape(8.dp))
    ) {

        Box(
            modifier = Modifier
                .size(46.dp)
                .background(if (selectedIndex == 2) ThemeRed.colorBorderSelect else ThemeRed.colorCommonBackground2)
                .clickable { onSelect(2) },

            contentAlignment = Alignment.Center
        )
        {
            Icon(
                painter = painterResource(R.drawable.select_2),
                contentDescription = null,
                tint = if (selectedIndex == 2) Color.White else ThemeRed.colorTextGray, modifier = Modifier.size(24.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(46.dp)
                .background(ThemeRed.colorBorderGray)
        )
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(if (selectedIndex == 1) ThemeRed.colorBorderSelect else ThemeRed.colorCommonBackground2)
                .clickable { onSelect(1) }, contentAlignment = Alignment.Center
        )
        {
            Icon(
                painter = painterResource(R.drawable.select_1),
                contentDescription = null,
                tint = if (selectedIndex == 1) Color.White else ThemeRed.colorTextGray, modifier = Modifier.size(24.dp)
            )
        }


    }


}




