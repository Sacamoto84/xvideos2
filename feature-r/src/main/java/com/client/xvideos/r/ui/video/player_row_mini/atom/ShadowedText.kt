package com.client.xvideos.r.ui.video.player_row_mini.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme

@Composable
fun ShadowedText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Text(
            text = text,
            color = Color.Black,
            modifier = Modifier.offset(1.dp, 1.dp),
            fontFamily = Theme.R.fontFamilyPopinsMedium
        )
        Text(
            text = text,
            color = Color.White,
            fontFamily = Theme.R.fontFamilyPopinsMedium
        )
    }
}

@Preview
@Composable
private fun ShadowedTextPreview() {
    ShadowedText(text = "12.5K")
}
