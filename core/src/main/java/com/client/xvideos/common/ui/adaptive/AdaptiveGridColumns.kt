package com.client.xvideos.common.ui.adaptive

import androidx.compose.runtime.Composable

/** Колонки сетки, у которой на телефоне их две. */
@Composable
fun adaptiveGridColumns(): Int = gridColumns(currentWidthClass())
