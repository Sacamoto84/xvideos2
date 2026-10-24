package com.client.xvideos.xlive.ui.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Плашка поверх потока XLive: какая комната играет.
 *
 * @param text ник комнаты, а при замене — почему показывается другая.
 */
@Composable
fun XLiveRoomBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 13.sp,
        modifier = modifier
            .background(Color(0x99000000), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Preview
@Composable
private fun XLiveRoomBadgePreview() {
    XLiveRoomBadge(text = "room_a: private · показываю room_b")
}
