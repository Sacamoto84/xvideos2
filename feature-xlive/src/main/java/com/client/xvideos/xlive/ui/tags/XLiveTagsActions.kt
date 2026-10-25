package com.client.xvideos.xlive.ui.tags

import androidx.compose.runtime.Immutable

/**
 * Действия таблицы «Теги» одним значением.
 *
 * @property onOpenTag тап по строке тега; аргумент — тег без `#`.
 * @property onOpenRoom тап по превью комнаты в строке; аргумент — ник.
 * @property onRetry «Повторить», когда не пришла первая страница.
 * @property onRetryLoadMore «Повторить», когда не пришла следующая страница.
 * @property onLoadMore список докрутился до конца.
 */
@Immutable
data class XLiveTagsActions(
    val onOpenTag: (String) -> Unit = {},
    val onOpenRoom: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRetryLoadMore: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
)
