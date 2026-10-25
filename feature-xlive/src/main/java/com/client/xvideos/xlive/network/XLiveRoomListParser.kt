package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveRoomItem
import com.client.xvideos.xlive.model.XLiveRoomPage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** Возраст, которым сайт отмечает «не указан». */
private const val AGE_NOT_SET = 99

/**
 * Страница списка из ответа `api/ts/roomlist/room-list/`.
 *
 * Комнаты без ника пропускаются. `total_count` сайт отдаёт строкой.
 *
 * @return `null`, если ответ не JSON-объект или в нём нет списка `rooms`
 * (например, ошибка параметров: `{"errors": {...}}`).
 */
fun parseXLiveRoomPage(json: String): XLiveRoomPage? {
    val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return null
    val rooms = root["rooms"] as? JsonArray ?: return null
    return XLiveRoomPage(
        rooms = rooms.mapNotNull { (it as? JsonObject)?.toRoomItem() },
        total = root.string("total_count")?.toIntOrNull() ?: 0,
    )
}

private fun JsonObject.toRoomItem(): XLiveRoomItem? {
    val username = string("username")?.takeIf { it.isNotBlank() } ?: return null
    return XLiveRoomItem(
        username = username,
        age = primitive("display_age")?.intOrNull?.takeIf { it < AGE_NOT_SET },
        gender = string("gender").orEmpty(),
        country = string("country").orEmpty(),
        viewers = primitive("num_users")?.intOrNull ?: 0,
        subject = string("room_subject").orEmpty().trim(),
        tags = (this["tags"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
            .orEmpty(),
        thumbnail = string("img").orEmpty(),
        isNew = primitive("is_new")?.booleanOrNull == true,
        isPromoted = string("label") == "promoted",
    )
}
