package com.client.xvideos.common.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalWindowInfo

/**
 * Ширина окна в целых dp; правило — [windowWidthDp].
 *
 * Окно, а не экран: в разделённом экране раскрытого складного окно уже экрана.
 */
@Composable
fun currentWindowWidthDp(): Int =
    windowWidthDp(LocalWindowInfo.current.containerDpSize.width, LocalConfiguration.current.screenWidthDp)
