package com.client.xvideos.r.ui.fullscreen.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.feature.r.R

@Composable
fun AbToggleButton(
    enableAB: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(46.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val color = if (enableAB) Color.Green else Color.LightGray
            Icon(
                painter = painterResource(R.drawable.rg_button),
                contentDescription = if (enableAB) "Выключить повтор отрезка A-B" else "Включить повтор отрезка A-B",
                tint = color
            )
            Text(
                "AB",
                color = color,
                fontSize = 8.sp,
                fontFamily = Theme.R.fontFamilyPopinsRegular
            )
        }
    }
}

@Preview
@Composable
private fun AbToggleButtonPreview() {
    AbToggleButton(
        enableAB = true,
        onClick = {}
    )
}
