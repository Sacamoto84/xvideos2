package com.client.xvideos.x.screens.tags.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.feature.saved.SavedLocalVideo

/** Действия с роликом в выдаче по тегу: открыть и пункты меню «три точки». */
@Immutable
data class TagCellActions(
    val onOpenVideo: (ItemsX) -> Unit,
    val isFavorite: (Long) -> Boolean,
    val onFavoriteAdd: (ItemsX) -> Unit,
    val onFavoriteRemove: (ItemsX) -> Unit,
    val onDownload: (ItemsX) -> Unit,
    val onSaveToGallery: (ItemsX) -> Unit,
    /** Скачанный файл ролика: превью и плеер из него; `null` — не сохранён. */
    val localVideoOf: (ItemsX) -> SavedLocalVideo? = { null },
) {
    companion object {
        /** Для превью: ничего не делает. */
        val None = TagCellActions({}, { false }, {}, {}, {}, {})
    }
}
