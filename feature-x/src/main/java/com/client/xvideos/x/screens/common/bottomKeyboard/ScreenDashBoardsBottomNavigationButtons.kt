package com.client.xvideos.x.screens.common.bottomKeyboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.client.xvideos.common.ui.keyboard.KeyboardNumber
import com.client.xvideos.common.ui.keyboard.KeyboardNumberTheme

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

/**
 * Нижняя панель навигации по страницам (`BottomListDashBoardNavigationButtons2`).
 *
 * Предоставляет:
 * - Кнопки со стрелками «<» и «>» для последовательного перехода.
 * - Горизонтальную прокручиваемую ленту номеров страниц.
 * - Кнопку вызова цифровой клавиатуры (иконка диапада) для прямого ввода целевой страницы.
 * - Открытие клавиатуры при тапе на текущую страницу или долгом нажатии на любой номер.
 *
 * @param value Индекс текущей страницы (0-based, т.е. 0 = Страница 1).
 * @param onChange Колбэк при выборе новой страницы (передаётся 0-based индекс).
 * @param max Общее количество доступных страниц (1-based, минимум 1).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomListDashBoardNavigationButtons2(
    value: Int,
    onChange: (Int) -> Unit,
    max: Int,
    modifier: Modifier = Modifier,
) {
    val safeMax = max.coerceAtLeast(1)
    val maxPageIndex = safeMax - 1

    var showKeyboardDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showKeyboardDialog) {
        showKeyboardDialog = false
    }

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
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ArrowNavigationButton(
            arrow = "<",
            enabled = value > 0,
            onClick = onBackClick
        )

        LazyRow(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = state
        ) {
            items(
                count = safeMax,
                key = { index -> index },
                contentType = { "page_number_item" }
            ) { index ->
                val onPageClick = remember(index, value, maxPageIndex, onChange) {
                    {
                        if (value == index) {
                            showKeyboardDialog = true
                        } else {
                            onChange(index.coerceIn(0, maxPageIndex))
                        }
                    }
                }
                val onPageLongClick = remember(index) {
                    {
                        showKeyboardDialog = true
                    }
                }
                PageNumberButton(
                    pageNumber = index + 1,
                    isSelected = value == index,
                    onClick = onPageClick,
                    onLongClick = onPageLongClick,
                    modifier = Modifier.fillParentMaxWidth(0.2f)
                )
            }
        }

        // Кнопка прямого ввода номера страницы
        Box(
            modifier = Modifier
                .padding(horizontal = 0.5.dp)
                .size(48.dp)
                .background(Color(0xFF2C2C2C))
                .clickable { showKeyboardDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Dialpad,
                contentDescription = "Ввести номер страницы",
                tint = Color(0xFFFF9900),
                modifier = Modifier.size(20.dp)
            )
        }

        ArrowNavigationButton(
            arrow = ">",
            enabled = value < maxPageIndex,
            onClick = onForwardClick
        )
    }

    // Диалог с цифровой клавиатурой
    if (showKeyboardDialog) {
        Dialog(onDismissRequest = { showKeyboardDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF3E3E3E), RoundedCornerShape(16.dp))
                    .background(Color(0xFF282828))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                KeyboardNumber(
                    theme = KeyboardNumberTheme(
                        colorBackground = Color(0xFF2D2D2D),
                        colorBorderBackground = Color(0xFF282828),
                        colorText = Color(0xFFFFFFFF),
                        buttonColor = Color(0xFF383838),
                        colorButtonBorder = Color(0xFF303030),
                    ),
                    value = -1,
                    max = safeMax,
                    onClick = { page1Based ->
                        onChange((page1Based - 1).coerceIn(0, maxPageIndex))
                        showKeyboardDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ArrowNavigationButton(
    arrow: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(horizontal = 0.5.dp)
            .size(48.dp)
            .background(if (!enabled) Color(0xFF2C2C2C) else Color(0xFFFF9000))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = arrow,
            color = if (!enabled) Color.DarkGray else Color.Black,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageNumberButton(
    pageNumber: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageText = remember(pageNumber) { pageNumber.toString() }
    val baseModifier = modifier
        .padding(horizontal = 0.5.dp)
        .height(48.dp)
        .background(Color(0xFF252525))
    val selectedModifier = if (isSelected) baseModifier.border(2.dp, Color(0xFFFF9900)) else baseModifier

    Box(
        modifier = selectedModifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = pageText, color = Color(0xFFCCCCCC))
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
