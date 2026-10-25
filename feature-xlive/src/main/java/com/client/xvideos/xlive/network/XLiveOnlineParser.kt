package com.client.xvideos.xlive.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull

/**
 * Признак эфира из ответа `api/online/<ник>/`: `{"online": true}`.
 *
 * @return `null`, если ответ не JSON-объект или в нём нет `online`.
 */
fun parseXLiveOnline(json: String): Boolean? {
    val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return null
    return root.primitive("online")?.booleanOrNull
}
