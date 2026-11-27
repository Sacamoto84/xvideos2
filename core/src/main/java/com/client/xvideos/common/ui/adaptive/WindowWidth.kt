package com.client.xvideos.common.ui.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.isSpecified

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
