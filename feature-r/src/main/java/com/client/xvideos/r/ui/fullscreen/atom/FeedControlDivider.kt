package com.client.xvideos.r.ui.fullscreen.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FeedControlDivider(
    modifier: Modifier = Modifier,
) {
    Spacer(modifier = modifier.height(8.dp).width(2.dp).background(Color.DarkGray))
}

@Preview
@Composable
private fun FeedControlDividerPreview() {
    FeedControlDivider()
}
