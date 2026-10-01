package com.client.xvideos.l.ui.screens.screenFullScreen.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SwipeableBottomPanel(
    modifier: Modifier = Modifier,
    contentHeight: Dp = 80.dp,
    indicatorHeight: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(contentHeight)
                .background(Color.DarkGray, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
        ) {
            // Индикатор-ручка
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = 8.dp)
                    .width(40.dp)
                    .height(indicatorHeight)
                    .offset(y = 2.dp)
                    .background(Color.Gray, RoundedCornerShape(2.dp))
            )

            content()
        }
    }
}

@Preview
@Composable
private fun SwipeableBottomPanelPreview() {
    SwipeableBottomPanel {
        Text("Panel Content", color = Color.White)
    }
}
