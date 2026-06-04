package com.client.xvideos.screens.common.bottomKeyboard

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

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

        BottomListDashBoardNavigationButtons2(value, {
            value = it
            println("!!! $it")
        }, 20000)

    }
}


//Черный цвет текста
private val colorTextBlack = Color(0xFF2C2C2C)

private val colorAccent = Color(0xFFFF9000)

private val colorTextWhite = Color(0xFFCCCCCC)

private val colorBlackBackground = Color(0xFF252525)

private val height = 48.dp

/**
 * Bottom navigation buttons
 * Навигация для переключения экранов, возвращает которая будет выбирать номер экрана
 * max - Максимальный индекс экрана
 * onChange - Функция вызывается при изменении экрана и передается номер экрана
 */
@Composable
fun BottomListDashBoardNavigationButtons2(value: Int, onChange: (Int) -> Unit, max: Int) {

    val navigator = LocalNavigator.currentOrThrow

    val list = remember { List(max) { it + 1 } }

    val state = rememberLazyListState()
    LaunchedEffect(value) {
        val indexToScroll = value + 1// Индекс, к которому нужно прокрутить
        val offset = calculateCenterOffset(state, indexToScroll)
        state.animateScrollToItem(index = indexToScroll, scrollOffset = -offset)
    }

    Row(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        
        ///////////////////////////////
        Box(
            modifier = Modifier
                .padding(horizontal = (0.5).dp)
                .width(height)
                .height(height)
                .background(
                    if (value == 0) colorTextBlack else colorAccent
                )
                .clickable {
                    onChange.invoke((value - 1).coerceAtLeast(0))
                }, contentAlignment = Alignment.Center
        ) {
            Text(
                "<",
                color = if (value == 0) Color.DarkGray else Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        ///////////////////////////////
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), state = state
        ) {

            items(list) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = (0.5).dp)
                        .fillParentMaxWidth(0.2f)
                        .height(height)
                        .border(
                            2.dp,
                            Color(if (value == it - 1) 0xFFFF9900 else 0x000000)
                        )
                        .background(colorBlackBackground)
                        .clickable {
                            onChange.invoke((it - 1).coerceAtLeast(0))
                        }, contentAlignment = Alignment.Center
                ) {
                    Text("$it", color = colorTextWhite)
                }
            }

        }

        MenuDot(
            modifier = Modifier.size(48.dp),
            (value + 1).coerceAtLeast(0),
            onChange = { onChange.invoke((it - 1).coerceAtLeast(0)) },
            max = max
        )

        ///////////////////////////////
        Box(
            modifier = Modifier
                .padding(horizontal = (0.5).dp)
                //.weight(1f)
                .width(height)
                .height(height)
                .background(if (value >= max - 1) colorTextBlack else colorAccent)
                .clickable {
                    onChange.invoke((value + 1).coerceIn(0, max))
                }, contentAlignment = Alignment.Center
        ) {
            Text(
                ">",
                color = if (value >= (max - 1)) Color.DarkGray else Color.Black,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

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

    return (viewportWidth + itemWidth) / 2 // Центр экрана минус половина ширины элемента
}