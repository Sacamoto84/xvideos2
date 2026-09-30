package com.client.xvideos.l.ui.screens.screenFullScreen.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.expandMenu.ExpandMenuType

@Immutable
data class FullScreenUiState(
    val item: PicsDetails,
    val filteredPic: List<PicsDetails>,
    val albumName: String,
    val idAlbum: String = "",
    val expandMenu: ExpandMenuType,
    val isCollection: Boolean = false,
    val autoPlay: Boolean = false,
    val currentIndex: Int = 0,
    val isFullScreen: Boolean = false,
    val showInfoDialog: Boolean = false,
    val verticalPager: Boolean = false,
    val videoMuted: Boolean = false,
    val resetZoomTrigger: Int = 0,
)
