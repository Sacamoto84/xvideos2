package com.client.xvideos.l.net.json

import kotlinx.serialization.json.Json

/**
 * Единый лояльный экземпляр Json для разбора сетевых ответов Luscious GraphQL и REST.
 *
 * - ignoreUnknownKeys = true: GraphQL API и схема Luscious могут возвращать новые поля.
 * - coerceInputValues = true: если сервер присылает null в поле со значением по умолчанию,
 *   подставляется значение по умолчанию вместо сбоя десериализации.
 * - isLenient = true: устойчивость к нестандартному форматированию JSON.
 * - encodeDefaults = true: сериализация сохраняет дефолтные значения полей.
 */
val LJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
    encodeDefaults = true
}
