package com.client.xvideos.screenRoot.molecule

import androidx.activity.compose.ReportDrawn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.screenRoot.ScreenRoot

@Composable
internal fun MainScreenContent(
    isStorageReady: Boolean,
    isAppLocked: Boolean,
) {
    if (LocalInspectionMode.current) return
    var contentOpened by remember { mutableStateOf(false) }
    if (!isStorageReady || (isAppLocked && !contentOpened)) return

    // При повторной блокировке сохраняем открытый стек навигации под заглушкой.
    SideEffect { contentOpened = true }
    ScreenRoot.Content()
    ReportDrawn()
}

@Preview
@Composable
private fun MainScreenContentPreview() {
    MainScreenContent(isStorageReady = true, isAppLocked = false)
}
