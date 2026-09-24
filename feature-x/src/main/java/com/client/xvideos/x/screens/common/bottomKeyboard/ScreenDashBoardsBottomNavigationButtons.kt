package com.client.xvideos.x.screens.common.bottomKeyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun ScreenDashBoardsBottomNavigationButtonsPreview() {

    var value by remember { mutableIntStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Gray),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(value.toString(), fontSize = 32.sp)

        BottomListDashBoardNavigationButtons2(value, { newValue ->
            value = newValue
            println("!!! $newValue")
        }, 20000)

    }
}

// Черный цвет текста
private val colorTextBlack = Color(0xFF2C2C2C)
private val colorAccent = Color(0xFFFF9000)
private val colorTextWhite = Color(0xFFCCCCCC)
private val colorBlackBackground = Color(0xFF252525)
private val selectedBorderColor = Color(0xFFFF9900)
private val navButtonHeight = 48.dp

/**
 * Bottom navigation buttons
 * Навигация для переключения экранов, возвращает которая будет выбирать номер экрана
 * max - Максимальный индекс экрана
 * onChange - Функция вызывается при изменении экрана и передается номер экрана
 */
@Composable
fun BottomListDashBoardNavigationButtons2(value: Int, onChange: (Int) -> Unit, max: Int) {

    val safeMax = max.coerceAtLeast(1)
    val maxPageIndex = safeMax - 1

    val state = rememberLazyListState()
    LaunchedEffect(value, safeMax) {
        val indexToScroll = value.coerceIn(0, maxPageIndex)
        val offset = calculateCenterOffset(state, indexToScroll)
        state.animateScrollToItem(index = indexToScroll, scrollOffset = -offset)
    }

    val onBackClick = remember(value, onChange) {
        { onChange((value - 1).coerceAtLeast(0)) }
    }
    val onForwardClick = remember(value, maxPageIndex, onChange) {
        { onChange((value + 1).coerceIn(0, maxPageIndex)) }
    }

    Row(
        modifier = Modifier
            .height(navButtonHeight)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val canGoBack = value > 0
        val backBg = if (!canGoBack) colorTextBlack else colorAccent
        val backTextColor = if (!canGoBack) Color.DarkGray else Color.Black

        Box(
            modifier = Modifier
                .padding(horizontal = 0.5.dp)
                .width(navButtonHeight)
                .height(navButtonHeight)
                .background(backBg)
                .clickable(enabled = canGoBack, onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "<",
                color = backTextColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = state
        ) {
            items(
                count = safeMax,
                key = { index -> index },
                contentType = { "page_number_item" }
            ) { index ->
                val onPageClick = remember(index, maxPageIndex, onChange) {
                    { onChange(index.coerceIn(0, maxPageIndex)) }
                }
                PageNumberButton(
                    pageNumber = index + 1,
                    isSelected = value == index,
                    onClick = onPageClick,
                    modifier = Modifier.fillParentMaxWidth(0.2f)
                )
            }
        }

        val canGoForward = value < maxPageIndex
        val forwardBg = if (!canGoForward) colorTextBlack else colorAccent
        val forwardTextColor = if (!canGoForward) Color.DarkGray else Color.Black

        Box(
            modifier = Modifier
                .padding(horizontal = 0.5.dp)
                .width(navButtonHeight)
                .height(navButtonHeight)
                .background(forwardBg)
                .clickable(enabled = canGoForward, onClick = onForwardClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                ">",
                color = forwardTextColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PageNumberButton(
    pageNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, selectedBorderColor)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .padding(horizontal = 0.5.dp)
            .height(navButtonHeight)
            .then(borderModifier)
            .background(colorBlackBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = pageNumber.toString(), color = colorTextWhite)
    }
}

// Функция для вычисления смещения, чтобы элемент был в центре экрана
fun calculateCenterOffset(state: LazyListState, index: Int): Int {
    val layoutInfo = state.layoutInfo
    val viewportWidth = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

    // Получаем информацию о конкретном элементе по индексу
    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

    // Если элемент видим, используем его ширину, иначе предполагаем стандартную ширину
    val itemWidth = itemInfo?.size ?: 0

    return calculateCenterOffset(viewportWidth, itemWidth)
}

internal fun calculateCenterOffset(viewportWidth: Int, itemWidth: Int): Int {
    return ((viewportWidth - itemWidth) / 2).coerceAtLeast(0) // Центр экрана минус половина ширины элемента
}
