package com.client.xvideos.xlive.ui.feed

import androidx.compose.runtime.Immutable

/**
 * Действия ленты «Эфиры» одним значением: сетке их нужно пять, и по
 * отдельности они раздували сигнатуры экрана и сетки.
 *
 * @property onOpenRoom тап по карточке; аргумент — ник комнаты.
 * @property onRetry «Повторить», когда не пришла первая страница.
 * @property onRetryLoadMore «Повторить», когда не пришла следующая страница.
 * @property onRefresh свайп вниз.
 * @property onLoadMore сетка докрутилась до конца.
 */
@Immutable
data class XLiveFeedActions(
    val onOpenRoom: (String) -> Unit = {},
    val onRetry: () -> Unit = {},
    val onRetryLoadMore: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onLoadMore: () -> Unit = {},
)
