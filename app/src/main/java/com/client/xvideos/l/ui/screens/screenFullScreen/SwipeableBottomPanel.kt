package com.client.xvideos.l.ui.screens.screenFullScreen

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.SwipeableState
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwipeableBottomPanel(
    modifier: Modifier = Modifier,
    contentHeight: Dp = 80.dp,
    swipeAreaHeight: Dp = 160.dp,
    indicatorHeight: Dp = 4.dp,
    content: @Composable (swipeableState: SwipeableState<Int>, hiddenOffset: Float) -> Unit
) {
    val contentHeightPx = with(LocalDensity.current) { contentHeight.toPx() }
    val indicatorHeightPx = with(LocalDensity.current) { indicatorHeight.toPx() }
    val swipeableState = rememberSwipeableState(initialValue = 0)

    val hiddenOffset = contentHeightPx - indicatorHeightPx - with(LocalDensity.current) { 4.dp.toPx() }
    val anchors = mapOf(0f to 0, hiddenOffset to 1)

    Box(modifier = modifier.fillMaxSize()) {
        // Сенсорная зона для свайпа
        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(swipeAreaHeight)
//                .swipeable(
//                    state = swipeableState,
//                    anchors = anchors,
//                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
//                    orientation = Orientation.Vertical
//                )
        )

        // Основная панель
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(contentHeight)
//                .offset {
//                    IntOffset(
//                        x = 0,
//                        y = swipeableState.offset.value.roundToInt()
//                    )
//                }
                .background(Color.DarkGray, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
        ) {
            // Индикатор
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = 8.dp)
                    .width(40.dp)
                    .height(indicatorHeight)
                    .offset(y = 2.dp)
                    .background(Color.Gray, RoundedCornerShape(2.dp))
            )

            content(swipeableState, hiddenOffset) // <-- передаём swipeableState внутрь контента
        }
    }
}


// Вариант с дополнительным индикатором для лучшей видимости
//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//fun SwipeableBottomPanel(
//    modifier: Modifier = Modifier,
//    contentHeight: Dp = 82.dp,
//    swipeAreaHeight: Dp = 180.dp,
//    indicatorHeight: Dp = 4.dp,
//    content: @Composable () -> Unit
//) {
//    val contentHeightPx = with(LocalDensity.current) { contentHeight.toPx() }
//    val indicatorHeightPx = with(LocalDensity.current) { indicatorHeight.toPx() }
//    val swipeableState = rememberSwipeableState(initialValue = 0)
//
//    val hiddenOffset = contentHeightPx - indicatorHeightPx - with(LocalDensity.current) { 3.dp.toPx() }
//
//    val anchors = mapOf(
//        0f to 0,
//        hiddenOffset to 1
//    )
//
//    Box(modifier = modifier.fillMaxSize()) {
//        // Сенсорная зона
//        Spacer(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .fillMaxWidth()
//                .height(swipeAreaHeight)
//                .swipeable(
//                    state = swipeableState,
//                    anchors = anchors,
//                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
//                    orientation = Orientation.Vertical
//                )
//        )
//
//        // Основная панель
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .fillMaxWidth()
//                .height(contentHeight)
//                .offset {
//                    IntOffset(
//                        x = 0,
//                        y = swipeableState.offset.value.roundToInt()
//                    )
//                }
//                .background(
//                    Color.DarkGray,
//                    RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
//                )
//        ) {
//            // Индикатор-полоска в верхней части панели (показывает, что можно свайпить)
//            Box(
//                modifier = Modifier
//                    .align(Alignment.TopCenter)
//                    .padding(bottom = 8.dp)
//                    .width(40.dp)
//                    .height(indicatorHeight)
//                    .offset(y = 2.dp)
//                    .background(
//                        Theme.L.grey2,
//                        RoundedCornerShape(2.dp)
//                    )
//            )
//
//            content()
//        }
//    }
//}


//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//fun SwipeableBottomPanel(
//    modifier: Modifier = Modifier,
//    contentHeight: Dp = 74.dp,
//    swipeAreaHeight: Dp = 180.dp,
//    content: @Composable () -> Unit
//) {
//    val contentHeightPx = with(LocalDensity.current) { contentHeight.toPx() }
//    val swipeableState = rememberSwipeableState(initialValue = 0)
//
//    val anchors = mapOf(
//        0f to 0,                    // панель видна
//        contentHeightPx to 1        // панель полностью скрыта
//    )
//
//    Box(modifier = modifier.fillMaxSize()) {
//        // Расширенная сенсорная зона
//        Spacer(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .fillMaxWidth()
//                .height(swipeAreaHeight) // 144.dp
//                .swipeable(
//                    state = swipeableState,
//                    anchors = anchors,
//                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
//                    orientation = Orientation.Vertical
//                )
//        )
//
//        // Визуальная панель
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .fillMaxWidth()
//                .height(contentHeight) // 72.dp
//                .offset {
//                    IntOffset(
//                        x = 0,
//                        y = swipeableState.offset.value.roundToInt()
//                    )
//                }
//                .background(
//                    Color.DarkGray,
//                    RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
//                )
//        ) {
//            content()
//        }
//    }
//}





//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//fun SwipeableBottomPanel(
//    modifier: Modifier = Modifier,
//    contentHeight: Dp = 74.dp,
//    touchHeight: Dp = 300.dp,
//    content: @Composable () -> Unit
//) {
//
//    val touchHeightPx = with(LocalDensity.current) { contentHeight.toPx() * 4 }
//
//    val swipeableState = rememberSwipeableState(initialValue = 0)
//
//    val anchors = mapOf(
//        0f to 0,                   // панель видна
//        touchHeightPx to 1          // панель скрыта
//    )
//
//    Box(modifier = modifier.fillMaxSize()) {
//        // "сенсорная зона"
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .fillMaxWidth()
//                .height(touchHeight)  // область, на которую реагирует свайп
//                .swipeable(
//                    state = swipeableState,
//                    anchors = anchors,
//                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
//                    orientation = Orientation.Vertical
//                )
//        ) {
//            // визуальная панель поверх сенсорной зоны
//            Box(
//                modifier = Modifier
//                    .align(Alignment.TopCenter)
//                    .fillMaxWidth()
//                    .height(contentHeight) // реальный контент 74.dp
//                    .offset {
//                        IntOffset(
//                            x = 0,
//                            y = swipeableState.offset.value.roundToInt()
//                        )
//                    }
//                    .background(Color.DarkGray, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
//            ) {
//                content()
//            }
//        }
//    }
//}


//@OptIn(ExperimentalMaterialApi::class)
//@Composable
//fun SwipeableBottomPanel(
//    modifier: Modifier = Modifier,
//    content: @Composable () -> Unit
//) {
//    val panelHeight = 76.dp
//    val panelHeightPx = with(LocalDensity.current) { panelHeight.toPx() }
//
//    val swipeableState = rememberSwipeableState(initialValue = 0)
//
//    val anchors = mapOf(
//        0f to 0,             // панель видна
//        panelHeightPx * 2 to 1   // панель скрыта (сдвинута вниз)
//    )
//
//    Box(
//        modifier = modifier.fillMaxSize()
//    ) {
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .fillMaxWidth()
//                .height(panelHeight)
//                .swipeable(
//                    state = swipeableState,
//                    anchors = anchors,
//                    thresholds = { _, _ -> FractionalThreshold(0.3f) },
//                    orientation = Orientation.Vertical
//                )
//                .offset {
//                    IntOffset(
//                        x = 0,
//                        y = swipeableState.offset.value.roundToInt()
//                    )
//                }
//                .background(Color.DarkGray, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
//        ) {
//            content()
//        }
//    }
//}
