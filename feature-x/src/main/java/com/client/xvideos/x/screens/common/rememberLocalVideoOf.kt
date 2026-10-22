package com.client.xvideos.x.screens.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.x.feature.saved.SavedLocalVideo
import com.client.xvideos.x.feature.saved.SavedX_Downloads
import com.client.xvideos.x.model.ItemsX

/**
 * Поиск скачанного файла для карточки: `null` — ролик не сохранён. Строится по
 * собранному списку сохранённых, поэтому ролик, докачавшийся при открытом
 * экране, сразу получает превью из файла и открывается скачанным.
 */
@Composable
fun rememberLocalVideoOf(downloads: SavedX_Downloads): (ItemsX) -> SavedLocalVideo? {
    val downloadedIds by downloads.downloadedVideoIds.collectAsStateWithLifecycle()
    return remember(downloadedIds, downloads) {
        { item -> if (item.id in downloadedIds) downloads.localVideo(item.id) else null }
    }
}
