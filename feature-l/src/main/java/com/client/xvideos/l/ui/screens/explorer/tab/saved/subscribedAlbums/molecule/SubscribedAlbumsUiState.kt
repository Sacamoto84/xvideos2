package com.client.xvideos.l.ui.screens.explorer.tab.saved.subscribedAlbums.molecule

import androidx.compose.runtime.Immutable
import com.client.xvideos.l.model.AlbumDetails

@Immutable
data class SubscribedAlbumsUiState(
    val albums: List<AlbumDetails> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)
