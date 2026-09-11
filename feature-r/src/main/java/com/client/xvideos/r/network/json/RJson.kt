package com.client.xvideos.r.network.json

import kotlinx.serialization.json.Json

/**
 * Единый лояльный экземпляр Json для разбора сетевых ответов RedGifs.
 *
 * - ignoreUnknownKeys = true: RedGifs API периодически расширяет ответы новыми полями.
 * - coerceInputValues = true: если сервер присылает null в поле со значением по умолчанию,
 *   подставляется значение по умолчанию вместо падения с ошибкой сериализации.
 * - isLenient = true: устойчивость к нестандартному форматированию JSON.
 * - encodeDefaults = true: сериализация сохраняет дефолтные значения полей.
 */
val RJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    encodeDefaults = true
}
