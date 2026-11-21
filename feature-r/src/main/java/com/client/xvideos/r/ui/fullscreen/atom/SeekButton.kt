package com.client.xvideos.r.ui.fullscreen.atom

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.ui.HintTooltip
import com.client.xvideos.feature.r.R

@Composable
fun SeekButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HintTooltip(text = contentDescription, modifier = modifier) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = Color.White
            )
        }
    }
}

@Preview
@Composable
private fun SeekButtonPreview() {
    SeekButton(
        iconRes = R.drawable.exo_icon_rewind,
        contentDescription = "Перемотать назад",
        onClick = {}
    )
}
