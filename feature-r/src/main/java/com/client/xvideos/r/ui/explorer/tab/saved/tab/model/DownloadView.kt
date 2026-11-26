package com.client.xvideos.r.ui.explorer.tab.saved.tab.model

import androidx.compose.ui.graphics.vector.ImageVector

internal enum class DownloadView {
    DETAILS,
    GRID,
}

internal data class DownloadMenuOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector,
)
