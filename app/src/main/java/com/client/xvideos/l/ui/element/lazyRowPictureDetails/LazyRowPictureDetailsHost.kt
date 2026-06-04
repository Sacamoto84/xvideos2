package com.client.xvideos.l.ui.element.lazyRowPictureDetails

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.PicsDetails

@OptIn(ExperimentalFoundationApi::class)
class LazyRowPictureDetailsHost(
    val albumName: String,
    val idAlbum: String = ""
) {

    val dpCacheWindow = LazyLayoutCacheWindow(ahead = 150.dp, behind = 100.dp)

    @OptIn(ExperimentalFoundationApi::class)
    val state = LazyStaggeredGridState()

    val state1 = LazyListState(cacheWindow = dpCacheWindow)

    /**
     * Количество отображаемых столбцов
     */
    var columns by mutableIntStateOf(3)

    var selectedImage by mutableStateOf<PicsDetails?>(null)

    var filteredPic =  mutableStateListOf<PicsDetails>()

    var collectionSearchQuery by mutableStateOf("")

    var collectionDuplicateDialogVisible by mutableStateOf(false)

    fun replaceFilteredPictures(items: List<PicsDetails>) {
        if (filteredPic.hasSameItems(items)) return

        filteredPic.clear()
        filteredPic.addAll(items)
    }

}

fun PicsDetails.selectionKey(): String {
    return url_to_original
        ?: url_to_video
        ?: thumbnails?.firstOrNull { !it.url.isNullOrBlank() }?.url
        ?: "${album.orEmpty()}-$width-$height-${is_animated}"
}

private fun List<PicsDetails>.hasSameItems(items: List<PicsDetails>): Boolean {
    if (size != items.size) return false
    return indices.all { index -> this[index].selectionKey() == items[index].selectionKey() }
}

