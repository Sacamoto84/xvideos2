package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveRoom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Комната из ответа `api/chatvideocontext/<ник>/`.
 *
 * Из семи десятков полей ответа нужны два: `room_status` и `hls_source`.
 * Остальные пропускаются, поэтому новые поля сайта разбор не ломают.
 *
 * @return `null`, если ответ не JSON-объект или в нём нет `room_status`.
 */
fun parseXLiveRoom(json: String): XLiveRoom? {
    val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return null
    val status = (root["room_status"] as? JsonPrimitive)?.contentOrNull ?: return null
    val hlsUrl = (root["hls_source"] as? JsonPrimitive)?.contentOrNull.orEmpty()
    return XLiveRoom(status = status, hlsUrl = hlsUrl)
}
