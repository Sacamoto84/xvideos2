package com.client.xvideos.common.ui.adaptive

import androidx.compose.runtime.Composable

@Composable
fun currentWidthClass(): WidthClass = widthClassOf(currentWindowWidthDp())
