package com.client.xvideos.r.ui.fullscreen

import androidx.paging.compose.LazyPagingItems
import com.client.xvideos.common.AppPath
import com.client.xvideos.r.common.downloader.downloadedVideoKey
import com.client.xvideos.r.model.GifsInfo

/**
 * Url элемента ленты по индексу для окна предзагрузки. `peek` (а не `get`) —
 * намеренно: он не дёргает пейджинг на подгрузку соседних страниц. Границу всё
 * равно проверяем: `peek` за пределами `itemCount` бросает IndexOutOfBoundsException.
 */
internal fun LazyPagingItems<GifsInfo>.peekUrl(index: Int, downloadedKeys: Set<String>): String? {
    if (index < 0 || index >= itemCount) return null
    return peek(index)?.let { redVideoUrl(it, downloadedKeys) }
}

/**
 * Адрес видео для элемента ленты: локальный файл, если ролик уже скачан,
 * иначе HLS с api.redgifs.com. Общая точка для страницы и для предзагрузки —
 * ключи preload-менеджера обязаны совпадать с тем, что реально играет плеер.
 *
 * Скачанность берётся из готового набора ключей, а не из `File.exists()`:
 * функция зовётся из окна предзагрузки на главном потоке для каждого индекса.
 */
internal fun redVideoUrl(item: GifsInfo, downloadedKeys: Set<String>): String =
    if (downloadedVideoKey(item.userName, item.id) in downloadedKeys) {
        "${AppPath.r_cache_download}/${item.userName}/${item.id}.mp4"
    } else {
        "https://api.redgifs.com/v2/gifs/${item.id.lowercase()}/hd.m3u8"
    }
