package com.client.xvideos.r.ui.video.player_row_mini.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.feature.r.R
import com.composables.core.Icon

@Composable
fun ShadowedIcon(
    modifier: Modifier = Modifier,
) {
    val painter = painterResource(R.drawable.rg_button)
    Box(modifier = modifier) {
        Icon(
            painter = painter,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.offset(1.dp, 1.dp)
        )
        Icon(
            painter = painter,
            contentDescription = "Просмотры",
            tint = Color.White
        )
    }
}

@Preview
@Composable
private fun ShadowedIconPreview() {
    ShadowedIcon()
}
