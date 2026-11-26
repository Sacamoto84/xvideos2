package com.client.xvideos.n.ui.screens.gallery.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.n.model.NTag

@Immutable
internal class NGallerySuccessActions(
    val onFavoriteClick: () -> Unit,
    val onServerFavoriteClick: () -> Unit,
    val onTagClick: (NTag) -> Unit,
    val onGalleryClick: (Long) -> Unit,
    val onRead: (Int) -> Unit,
)
