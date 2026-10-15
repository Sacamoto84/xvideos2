package com.client.xvideos.common.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified

/**
 * Ширина окна в целых dp; правило — [windowWidthDp].
 *
 * Окно, а не экран: в разделённом экране раскрытого складного окно уже экрана.
 */
@Composable
fun currentWindowWidthDp(): Int =
    windowWidthDp(LocalWindowInfo.current.containerDpSize.width, LocalConfiguration.current.screenWidthDp)

/**
 * Ширина окна в целых dp из размера окна [containerWidth] и
 * `Configuration.screenWidthDp` ([configWidthDp]).
 *
 * Класс ширины решает окно, округлённое вниз. Когда Compact и окно, и
 * конфигурация, берётся конфигурация: на телефоне значения ровно прежние, а
 * `screenWidthDp` на новых API округляется к ближайшему, не вниз. Во время
 * сложения или раскрытия конфигурация может отставать от окна — тогда она
 * класс не меняет. Без окна (редкие превью) — конфигурация.
 */
fun windowWidthDp(containerWidth: Dp, configWidthDp: Int): Int {
    if (!containerWidth.isSpecified) return configWidthDp
    val windowDp = containerWidth.value.toInt()
    val bothCompact = widthClassOf(windowDp) == WidthClass.Compact &&
        widthClassOf(configWidthDp) == WidthClass.Compact
    return if (bothCompact) configWidthDp else windowDp
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
