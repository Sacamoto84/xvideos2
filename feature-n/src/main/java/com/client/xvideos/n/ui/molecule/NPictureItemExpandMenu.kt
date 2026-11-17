package com.client.xvideos.n.ui.molecule

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.common.expandmenu.LazyExpandMenuAnchor
import com.client.xvideos.common.share.useCaseShareText
import com.client.xvideos.n.model.NPicture

/**
 * Выпадающее меню действий для отдельной картинки манги (кнопка «три точки»),
 * унифицированное с поведением и дизайном раздела L.
 */
@Composable
fun NPictureItemExpandMenu(
    picture: NPicture,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    onOpenGallery: (() -> Unit)? = null,
    onToggleLike: (() -> Unit)? = null,
    onAddToCollection: (() -> Unit)? = null,
    onRemoveFromCollection: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null
) {
    val context = LocalContext.current

    LazyExpandMenuAnchor(
        modifier = modifier,
        menuWidth = IntrinsicSize.Max
    ) { dismiss ->
        if (onOpenGallery != null) {
            ExpandMenuActionItem(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                text = "Открыть альбом"
            ) {
                dismiss()
                onOpenGallery()
            }
        }

        if (onToggleLike != null) {
            val likeIcon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
            val likeText = if (isLiked) "Удалить из лайков" else "В лайки"
            ExpandMenuActionItem(icon = likeIcon, text = likeText) {
                dismiss()
                onToggleLike()
            }
        }

        if (onAddToCollection != null) {
            ExpandMenuActionItem(
                icon = Icons.Outlined.CreateNewFolder,
                text = "В коллекцию"
            ) {
                dismiss()
                onAddToCollection()
            }
        }

        val shareAction = onShare ?: {
            val shareText = if (picture.galleryTitle.isNotEmpty()) {
                "${picture.galleryTitle} (Стр. ${picture.pageNumber})\n${picture.imageUrl}"
            } else {
                picture.imageUrl
            }
            useCaseShareText(context, shareText, picture.galleryTitle.ifEmpty { null })
        }

        ExpandMenuActionItem(
            icon = Icons.Default.Share,
            text = "Поделиться"
        ) {
            dismiss()
            shareAction()
        }

        if (onRemoveFromCollection != null) {
            ExpandMenuActionItem(
                icon = Icons.Filled.DeleteOutline,
                text = "Удалить из коллекции"
            ) {
                dismiss()
                onRemoveFromCollection()
            }
        }

        if (onDelete != null) {
            ExpandMenuActionItem(
                icon = Icons.Filled.DeleteOutline,
                text = "Удалить"
            ) {
                dismiss()
                onDelete()
            }
        }
    }
}

@Preview
@Composable
private fun NPictureItemExpandMenuPreview() {
    NPictureItemExpandMenu(
        picture = NPicture(
            id = "1_1",
            galleryId = 1L,
            mediaId = 1L,
            pageNumber = 1,
            galleryTitle = "Preview Manga"
        ),
        isLiked = true,
        onOpenGallery = {},
        onToggleLike = {},
        onAddToCollection = {},
        onDelete = {}
    )
}
