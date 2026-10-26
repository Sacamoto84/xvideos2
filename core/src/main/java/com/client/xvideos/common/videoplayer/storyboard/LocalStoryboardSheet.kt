package com.client.xvideos.common.videoplayer.storyboard

import androidx.compose.ui.unit.IntOffset

/**
 * Сетка кадров скачанного ролика X — та же раскладка, что у раскадровки сайта
 * ([STORYBOARD_COLUMNS] × [STORYBOARD_ROWS]): 10×10 клеток, кадр `i` — `i`% длины ролика, по
 * строкам слева направо. Собирается из самого файла, клетка 192×108, как у
 * сайта: маленький кадр превью с таким размером смотрится нормально.
 */
const val LOCAL_STORYBOARD_CELL_WIDTH = 192
const val LOCAL_STORYBOARD_CELL_HEIGHT = 108
const val LOCAL_STORYBOARD_WIDTH = LOCAL_STORYBOARD_CELL_WIDTH * STORYBOARD_COLUMNS
const val LOCAL_STORYBOARD_HEIGHT = LOCAL_STORYBOARD_CELL_HEIGHT * STORYBOARD_ROWS

private const val PERCENT = 100L
private const val US_PER_MS = 1000L

/**
 * Файл сетки ролика [id] рядом с `<id>.mp4`. Имя без расширения — не число:
 * обход каталога загрузок не примет сетку за обложку `<id>.jpg`.
 */
fun localStoryboardFileName(id: Long): String = "$id.storyboard.jpg"

/** Время кадра [index] в микросекундах — начало его процента длины [durationMs]. */
fun storyboardFrameTimeUs(index: Int, durationMs: Long): Long = durationMs * US_PER_MS * index / PERCENT

/**
 * Левый верхний угол кадра [index] размером [frameWidth]×[frameHeight] в
 * картинке-сетке: кадр по центру своей клетки, с полями, если пропорции не
 * совпадают; кадр больше клетки начинается в её углу.
 */
fun storyboardCellOrigin(index: Int, frameWidth: Int, frameHeight: Int): IntOffset {
    val column = index % STORYBOARD_COLUMNS
    val row = index / STORYBOARD_COLUMNS
    val x = column * LOCAL_STORYBOARD_CELL_WIDTH + ((LOCAL_STORYBOARD_CELL_WIDTH - frameWidth) / 2).coerceAtLeast(0)
    val y = row * LOCAL_STORYBOARD_CELL_HEIGHT + ((LOCAL_STORYBOARD_CELL_HEIGHT - frameHeight) / 2).coerceAtLeast(0)
    return IntOffset(x, y)
}
