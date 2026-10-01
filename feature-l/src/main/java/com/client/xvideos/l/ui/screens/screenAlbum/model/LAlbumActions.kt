package com.client.xvideos.l.ui.screens.screenAlbum.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.l.model.AlbumDetails

/**
 * Действия экрана альбома. Экран связывает их со ScreenLAlbumSM,
 * молекулы вызывают колбэки и про ScreenModel не знают.
 */
@Immutable
data class LAlbumActions(
    val onSaveAlbum: () -> Unit = {},
    val onToggleServerFavorite: (AlbumDetails) -> Unit = {},
    val onShareAlbum: (AlbumDetails) -> Unit = {},
    val onShowOnlyAnimatedChange: (Boolean) -> Unit = {},
    val onSyncServerFavoriteStatus: (String?) -> Unit = {},
    val onRetryFailedPages: () -> Unit = {},
    val onRefresh: () -> Unit = {},
)
