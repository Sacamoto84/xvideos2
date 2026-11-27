package com.client.xvideos.common.ui.adaptive

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

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
