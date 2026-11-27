package com.client.xvideos.common.ui.adaptive

import androidx.compose.runtime.Composable

/** Колонки сетки с выбором пользователя: [selected] плюс запас широкого окна. */
@Composable
fun adaptiveUserGridColumns(selected: Int): Int = userGridColumns(selected, currentWidthClass())
