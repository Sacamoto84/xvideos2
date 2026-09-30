package com.client.xvideos.x.screens.channel.atom

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.feature.x.R

private val FLAG_FONT = FontFamily(Font(R.font.flag))

@Composable
fun CountryFlagTag(
    flagEmoji: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = flagEmoji,
        fontFamily = FLAG_FONT,
        fontSize = 18.sp,
        modifier = modifier.padding(end = 6.dp)
    )
}

@Preview
@Composable
private fun CountryFlagTagPreview() {
    CountryFlagTag(flagEmoji = "🇧🇷")
}
