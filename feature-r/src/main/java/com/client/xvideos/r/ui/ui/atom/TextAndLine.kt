package com.client.xvideos.r.ui.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme

@Composable
fun TextAndLine(
    str: String,
    select: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textColor = if (select) Color.White else Theme.R.colorTextGray
    val indicatorColor = if (select) Theme.R.colorRed else Color.Transparent

    Box(
        modifier = modifier
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = str,
            fontSize = 18.sp,
            color = textColor,
            fontFamily = Theme.R.fontFamilyPopinsRegular
        )

        Box(
            modifier = Modifier
                .offset(0.dp, 16.dp)
                .width(48.dp)
                .height(4.dp)
                .background(indicatorColor)
        )
    }
}

@Preview
@Composable
private fun TextAndLinePreviewSelected() {
    Box(modifier = Modifier.background(Theme.background)) {
        TextAndLine(
            str = "Gifs",
            select = true,
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun TextAndLinePreviewUnselected() {
    Box(modifier = Modifier.background(Theme.background)) {
        TextAndLine(
            str = "Images",
            select = false,
            onClick = {}
        )
    }
}
