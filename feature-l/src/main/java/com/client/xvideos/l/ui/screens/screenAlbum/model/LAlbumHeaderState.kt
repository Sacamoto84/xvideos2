package com.client.xvideos.l.ui.screens.screenAlbum.model

import androidx.compose.runtime.Immutable

/**
 * Состояние кнопок шапки альбома. Экран снимает его со ScreenLAlbumSM,
 * молекулы получают готовые значения.
 */
@Immutable
data class LAlbumHeaderState(
    val isServerFavorite: Boolean? = null,
    val isServerFavoriteLoading: Boolean = false,
    val showOnlyAnimated: Boolean = false,
)
