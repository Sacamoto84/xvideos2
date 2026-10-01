package com.client.xvideos.l.ui.element.expandMenu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.model.lDownloadUrl
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.LazyRowPictureDetailsHost

@Composable
fun ExpandMenu(
    type: ExpandMenuType,
    item: PicsDetails,
    idAlbum: String,
    viewModel: ExpandMenuViewModel,
    modifier: Modifier = Modifier,
    isCollection: Boolean = false,
    host: LazyRowPictureDetailsHost? = null
) {
    when (type) {
        ExpandMenuType.NONE -> {}
        ExpandMenuType.ALBUM -> {
            val album = idAlbum.toLongOrNull() ?: 0L
            AlbumItemExpandMenu(
                item = item,
                modifier = modifier,
                onDownload = { it1 -> viewModel.downloadLike(it1, album) },
                onServerLike = { it1 -> viewModel.likeOnServer(it1) },
                onShare = { it1 -> viewModel.onShareClicked(it1) },
                onSaveToGallery = { it1 -> viewModel.saveToGallery(it1) },
                isCollection = isCollection,
                savedL = viewModel.saved,
                onRemoveFromCollection = { },
                idAlbum = idAlbum
            )
        }
        ExpandMenuType.LIKES -> {
            val haptic = LocalHapticFeedback.current
            SavedLikesItemExpandMenu(
                item = item,
                modifier = modifier,
                onDelete = {
                    val url = item.url_to_original ?: item.url_to_video ?: item.lDownloadUrl()
                    url?.let { viewModel.saved.likes.remove(it) }
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                onServerLike = { it1 -> viewModel.likeOnServer(it1) },
                onShare = { it1 -> viewModel.onShareClicked(it1) },
                onSaveToGallery = { it1 -> viewModel.saveToGallery(it1) },
                isCollection = isCollection,
                savedL = viewModel.saved,
                onRemoveFromCollection = { }
            )
        }
        ExpandMenuType.SERVER_LIKES -> ServerLikesItemExpandMenu(
            item = item,
            modifier = modifier,
            onDownload = { it1 ->
                val album = idAlbum.toLongOrNull() ?: it1.album?.toLongOrNull() ?: 0L
                viewModel.downloadLike(it1, album)
            },
            onServerUnlike = { it1 ->
                viewModel.unlikeOnServer(it1) {
                    host?.removePicture(it1)
                }
            },
            onShare = { it1 -> viewModel.onShareClicked(it1) },
            onSaveToGallery = { it1 -> viewModel.saveToGallery(it1) },
            savedL = viewModel.saved,
            idAlbum = idAlbum
        )
    }
}
