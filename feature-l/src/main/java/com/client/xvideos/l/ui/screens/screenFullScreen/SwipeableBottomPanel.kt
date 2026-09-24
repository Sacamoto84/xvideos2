package com.client.xvideos.l.ui.screens.screenFullScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val PANEL_TOP_SHAPE = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
private val HANDLE_SHAPE = RoundedCornerShape(2.dp)
private val DEFAULT_CONTENT_HEIGHT = 80.dp
private val DEFAULT_INDICATOR_HEIGHT = 4.dp
private val HANDLE_WIDTH = 40.dp
private val HANDLE_OFFSET_Y = 2.dp
private val HANDLE_BOTTOM_PADDING = 8.dp
private val PANEL_BG_COLOR = Color.DarkGray
private val HANDLE_BG_COLOR = Color.Gray

/**
 * Нижняя панель для полноэкранного просмотра картинок.
 */
@Composable
fun SwipeableBottomPanel(
    modifier: Modifier = Modifier,
    contentHeight: Dp = DEFAULT_CONTENT_HEIGHT,
    indicatorHeight: Dp = DEFAULT_INDICATOR_HEIGHT,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(contentHeight)
                .background(PANEL_BG_COLOR, PANEL_TOP_SHAPE)
        ) {
            // Индикатор-ручка
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = HANDLE_BOTTOM_PADDING)
                    .width(HANDLE_WIDTH)
                    .height(indicatorHeight)
                    .offset(y = HANDLE_OFFSET_Y)
                    .background(HANDLE_BG_COLOR, HANDLE_SHAPE)
            )

            content()
        }
    }
}
