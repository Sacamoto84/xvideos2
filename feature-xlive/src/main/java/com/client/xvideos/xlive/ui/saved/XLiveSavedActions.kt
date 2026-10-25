package com.client.xvideos.xlive.ui.saved

import androidx.compose.runtime.Immutable

/**
 * Действия сетки «Моих» и «Истории».
 *
 * @property onOpenRoom тап по карточке; аргумент — ник комнаты.
 * @property onRemove убрать комнату из показанного списка — после подтверждения.
 * @property onRefresh свайп вниз: проверить эфир заново, обновить снимки.
 */
@Immutable
data class XLiveSavedActions(
    val onOpenRoom: (String) -> Unit = {},
    val onRemove: (String) -> Unit = {},
    val onRefresh: () -> Unit = {},
)
