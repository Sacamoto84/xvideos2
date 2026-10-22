package com.client.xvideos.x.screens.common

import cafe.adriel.voyager.core.screen.Screen
import com.client.xvideos.x.feature.saved.SavedLocalVideo
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.screens.videoplayer.ScreenX_LocalVideoPlayer
import com.client.xvideos.x.screens.videoplayer.ScreenX_VideoPlayer

/**
 * Экран плеера для ролика [item] по удержанию и двойному тапу карточки:
 * сохранённый ([local] не `null`) — скачанный файл, остальные — из сети.
 */
fun playerScreenFor(item: ItemsX, local: SavedLocalVideo?): Screen =
    if (local != null) {
        ScreenX_LocalVideoPlayer(local.fileUrl, item)
    } else {
        ScreenX_VideoPlayer(normalizeXUrl(item.href), item)
    }
