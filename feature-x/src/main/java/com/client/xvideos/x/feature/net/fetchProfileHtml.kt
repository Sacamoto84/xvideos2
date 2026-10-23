package com.client.xvideos.x.feature.net

import com.client.xvideos.x.urlStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Загружает HTML-разметку страницы канала или модели с fallback-проверками альтернативных путей.
 *
 * Пустая строка — ни один адрес не ответил (контракт [readHtmlFromURLDirect]).
 */
suspend fun fetchProfileHtml(cleanSlug: String, isModel: Boolean): String = withContext(Dispatchers.IO) {
    val pathPrefix = if (isModel) "models" else "channels"
    val raw = readHtmlFromURLDirect("$urlStart/$pathPrefix/$cleanSlug")
    if (raw.isNotBlank() && !raw.contains("Не найдено")) return@withContext raw

    val altPrefix = if (isModel) "channels" else "models"
    val rawAlt = readHtmlFromURLDirect("$urlStart/$altPrefix/$cleanSlug")
    if (rawAlt.isNotBlank() && !rawAlt.contains("Не найдено")) return@withContext rawAlt

    val rawProfile = readHtmlFromURLDirect("$urlStart/profiles/$cleanSlug")
    if (rawProfile.isNotBlank() && !rawProfile.contains("Не найдено")) return@withContext rawProfile

    readHtmlFromURLDirect("$urlStart/$cleanSlug")
}
