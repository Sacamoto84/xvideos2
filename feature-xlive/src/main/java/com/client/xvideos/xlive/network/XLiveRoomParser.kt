package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveRoom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Комната из ответа `api/chatvideocontext/<ник>/`.
 *
 * Из семи десятков полей ответа берутся статус, поток и то, что видно в
 * шапке комнаты. Остальные пропускаются, поэтому новые поля сайта разбор не
 * ломают.
 *
 * @return `null`, если ответ не JSON-объект или в нём нет `room_status`.
 */
fun parseXLiveRoom(json: String): XLiveRoom? {
    val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return null
    val status = root.string("room_status") ?: return null
    return XLiveRoom(
        status = status,
        hlsUrl = root.string("hls_source").orEmpty(),
        username = root.string("broadcaster_username").orEmpty(),
        title = root.string("room_title").orEmpty(),
        viewers = root.primitive("num_viewers")?.intOrNull ?: 0,
        gender = root.string("broadcaster_gender").orEmpty(),
        startedAt = root.primitive("start_timestamp")?.longOrNull ?: 0L,
        ratingPercent = (root["satisfaction_score"] as? JsonObject)?.primitive("percent")?.intOrNull,
    )
}

internal fun JsonObject.primitive(key: String): JsonPrimitive? = this[key] as? JsonPrimitive

internal fun JsonObject.string(key: String): String? = primitive(key)?.contentOrNull
