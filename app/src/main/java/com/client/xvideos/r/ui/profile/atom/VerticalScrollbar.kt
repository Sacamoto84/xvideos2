package com.client.xvideos.r.ui.profile.atom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VerticalScrollbar(scrollPercent: Pair<Float, Float>) { // Пример параметра

    BoxWithConstraints (
        modifier = Modifier
            .fillMaxSize()
    ) {
        val trackTotalHeightPx = this@BoxWithConstraints.constraints.maxHeight
        val trackTotalWidthPx = this@BoxWithConstraints.constraints.maxWidth

        val currentRange = scrollPercent

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Убедимся, что start не больше end, и есть что отображать
            if (currentRange.second > currentRange.first && trackTotalHeightPx > 0) {
                val startFraction = currentRange.first.coerceIn(0f, 1f)
                val endFraction = currentRange.second.coerceIn(0f, 1f)

                val indicatorOffsetYPx: Float = trackTotalHeightPx * startFraction
                // Рассчитываем высоту видимой части в пикселях
                val indicatorHeightPx: Float = (trackTotalHeightPx * (endFraction - startFraction))
                    .coerceAtLeast(0f) // Высота не может быть отрицательной

                // Предохранитель, чтобы индикатор не вышел за пределы трека снизу
                val actualIndicatorHeightPx = indicatorHeightPx.coerceAtMost(trackTotalHeightPx - indicatorOffsetYPx)

                if (actualIndicatorHeightPx > 0f) { // Рисуем только если есть реальная высота
                    drawRect(
                        color = Color.Gray, // Цвет вашего индикатора
                        topLeft = Offset(x = 0f, y = indicatorOffsetYPx),
                        size = Size(width = trackTotalWidthPx.toFloat(), height = actualIndicatorHeightPx)
                    )
                }
            }
        }
    }
}


// Более простая и правильная версия
@Composable
fun VerticalScrollbar2(scrollPercent: Pair<Float, Float>) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val trackTotalHeightPx = this@BoxWithConstraints.constraints.maxHeight

        val currentRange = scrollPercent

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (currentRange.second > currentRange.first && trackTotalHeightPx > 0) {
                val startFraction = currentRange.first.coerceIn(0f, 1f)
                val endFraction = currentRange.second.coerceIn(0f, 1f)

                val rangeCenter = (startFraction + endFraction) / 2f

                val centerFraction = if (rangeCenter <= 0.5f) {
                    // В первой половине: интерполируем от 0 до 1 (но максимум до rangeCenter * 2)
                    // Когда startFraction = 0 и rangeCenter близок к 0 -> результат близок к 0
                    // Когда rangeCenter = 0.5 -> результат = 0.5
                    val t = rangeCenter / 0.5f // нормализуем [0, 0.5] в [0, 1]
                    startFraction * (1f - t) + rangeCenter * t
                } else {
                    // Во второй половине: интерполируем от rangeCenter до endFraction
                    // Когда rangeCenter = 0.5 -> результат = 0.5
                    // Когда endFraction = 1 и rangeCenter близок к 1 -> результат близок к 1
                    val t = (rangeCenter - 0.5f) / 0.5f // нормализуем [0.5, 1] в [0, 1]
                    rangeCenter * (1f - t) + endFraction * t
                }

                // Позиция центра круга по Y
                val circleCenterY = trackTotalHeightPx * centerFraction

                // Рисуем круг
                drawCircle(
                    color = Color.Magenta,
                    radius = 1.dp.toPx(),
                    center = Offset(x = 0f, y = circleCenterY)
                )
            }
        }
    }
}



//@Composable
//fun VerticalScrollbar2(scrollPercent: Pair<Float, Float>) {
//    BoxWithConstraints(
//        modifier = Modifier.fillMaxSize()
//    ) {
//        val trackTotalHeightPx = this@BoxWithConstraints.constraints.maxHeight
//
//        val currentRange = scrollPercent
//
//        Canvas(modifier = Modifier.fillMaxSize()) {
//            // Убедимся, что start не больше end, и есть что отображать
//            if (currentRange.second > currentRange.first && trackTotalHeightPx > 0) {
//                val startFraction = currentRange.first.coerceIn(0f, 1f)
//                val endFraction = currentRange.second.coerceIn(0f, 1f)
//
//                // Вычисляем центр видимого диапазона
//                val centerFraction = (startFraction + endFraction) / 2f
//
//                // Позиция центра круга по Y
//                val circleCenterY = trackTotalHeightPx * centerFraction
//
//                // Рисуем круг
//                drawCircle(
//                    color = Color.Magenta,
//                    radius = 1.dp.toPx(),
//                    center = Offset(x = 0f, y = circleCenterY)
//                )
//            }
//        }
//    }
//}