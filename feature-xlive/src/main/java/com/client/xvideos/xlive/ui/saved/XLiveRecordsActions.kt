package com.client.xvideos.xlive.ui.saved

import androidx.compose.runtime.Immutable

/**
 * Действия сетки «Записей».
 *
 * @property onOpen тап по карточке: экран записи; аргумент — id записи.
 * @property onRemove удалить запись — после подтверждения.
 * @property onSaveToGallery кнопка на карточке готовой записи: видео в общую галерею.
 */
@Immutable
data class XLiveRecordsActions(
    val onOpen: (Long) -> Unit = {},
    val onRemove: (Long) -> Unit = {},
    val onSaveToGallery: (Long) -> Unit = {},
)
