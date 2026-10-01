package com.client.xvideos.l.ui.element.expandMenu

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
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
        ExpandMenuType.ALBUM -> ExpandMenuAlbum(
            item = item,
            idAlbum = idAlbum,
            viewModel = viewModel,
            modifier = modifier,
            isCollection = isCollection
        )
        ExpandMenuType.LIKES -> ExpandMenuLikes(
            item = item,
            viewModel = viewModel,
            modifier = modifier,
            isCollection = isCollection
        )
        ExpandMenuType.SERVER_LIKES -> ExpandMenuServerLikes(
            item = item,
            idAlbum = idAlbum,
            viewModel = viewModel,
            modifier = modifier,
            host = host
        )
    }
}

@Composable
fun ExpandMenuAlbum(
    item: PicsDetails,
    idAlbum: String,
    viewModel: ExpandMenuViewModel,
    modifier: Modifier = Modifier,
    isCollection: Boolean = false
) {
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

@Composable
fun ExpandMenuLikes(
    item: PicsDetails,
    viewModel: ExpandMenuViewModel,
    modifier: Modifier = Modifier,
    isCollection: Boolean = false
) {
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

@Composable
fun ExpandMenuServerLikes(
    item: PicsDetails,
    idAlbum: String,
    viewModel: ExpandMenuViewModel,
    modifier: Modifier = Modifier,
    host: LazyRowPictureDetailsHost? = null
) {
    ServerLikesItemExpandMenu(
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

@Preview(showBackground = true, backgroundColor = 0xFF222222)
@Composable
private fun ExpandMenuPreview() {
    AlbumItemExpandMenu(
        item = PicsDetails(
            url_to_original = "https://example.com/test.jpg",
            url_to_video = null,
            width = 800,
            height = 600
        )
    )
}
