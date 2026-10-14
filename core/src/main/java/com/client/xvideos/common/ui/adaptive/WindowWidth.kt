package com.client.xvideos.common.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Ширина окна в целых dp, округлённая вниз — как `Configuration.screenWidthDp`,
 * поэтому на телефоне в портрете значения совпадают.
 *
 * Окно, а не экран: в разделённом экране раскрытого складного окно уже экрана.
 * Без окна (редкие превью) — ширина из конфигурации, как было до адаптации.
 */
@Composable
fun currentWindowWidthDp(): Int {
    val width = LocalWindowInfo.current.containerDpSize.width
    return if (width.isSpecified) width.value.toInt() else LocalConfiguration.current.screenWidthDp
}

@Composable
fun currentWidthClass(): WidthClass = widthClassOf(currentWindowWidthDp())

/** Колонки сетки, у которой на телефоне их две. */
@Composable
fun adaptiveGridColumns(): Int = gridColumns(currentWidthClass())

/** Колонки сетки с выбором пользователя: [selected] плюс запас широкого окна. */
@Composable
fun adaptiveUserGridColumns(selected: Int): Int = userGridColumns(selected, currentWidthClass())

/** Ширина карточки горизонтального ряда: (окно − [gap]) / видимых карточек. */
@Composable
fun rowItemWidth(gap: Dp = 8.dp): Dp {
    val widthDp = currentWindowWidthDp()
    return (widthDp.dp - gap) / rowVisibleItems(widthClassOf(widthDp))
}

/**
 * `contentPadding` списка на всю ширину окна: [base] плюс боковой отступ,
 * который держит строки не шире [CONTENT_MAX_WIDTH_DP]. Список при этом
 * свайпается по всей ширине. На телефоне возвращает сам [base].
 *
 * Только для списков на всю ширину окна: отступ считается от окна, а не от
 * родителя.
 */
@Composable
fun adaptiveListPadding(base: PaddingValues = PaddingValues(0.dp)): PaddingValues =
    base.plusSides(contentSidePaddingDp(currentWindowWidthDp()).dp)
