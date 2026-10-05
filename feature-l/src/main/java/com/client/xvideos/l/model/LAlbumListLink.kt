package com.client.xvideos.l.model

import com.client.xvideos.l.model.enum.AlbumType
import com.client.xvideos.l.model.enum.ContentId
import java.net.URLDecoder

/**
 * Параметры ссылки сайта на список альбомов, например
 * `/albums/list/?album_type=manga&display=date_trending&tagged=%2Bcollared`.
 *
 * Такую ссылку несёт каждый раздел подборки — это его «показать все». Пары без
 * имени или без `=` пропускаются.
 */
internal fun lAlbumListLinkParams(url: String): Map<String, String> =
    url.substringAfter('?', "")
        .split('&')
        .mapNotNull { pair ->
            val separatorIndex = pair.indexOf('=')
            if (separatorIndex <= 0) return@mapNotNull null
            val name = URLDecoder.decode(pair.substring(0, separatorIndex), "UTF-8")
            val value = URLDecoder.decode(pair.substring(separatorIndex + 1), "UTF-8")
            name to value
        }
        .toMap()

/** Тип альбома из параметров ссылки; `null`, если его нет или значение незнакомо. */
internal fun Map<String, String>.albumTypeOrNull(): AlbumType? =
    this["album_type"]?.let { value -> AlbumType.entries.find { it.value == value } }

/** Тип контента из параметров ссылки; `null`, если его нет или значение незнакомо. */
internal fun Map<String, String>.contentIdOrNull(): ContentId? =
    this["content_id"]?.toIntOrNull()?.let { value -> ContentId.entries.find { it.value == value } }
