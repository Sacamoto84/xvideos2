package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveTag
import com.client.xvideos.xlive.model.XLiveTagPage
import com.client.xvideos.xlive.model.XLiveTagRoom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull

/**
 * Страница таблицы тегов из ответа `api/ts/hashtags/tag-table-data/`.
 *
 * Теги без имени пропускаются, комнаты превью без ника — тоже.
 *
 * @return `null`, если ответ не JSON-объект или в нём нет списка `hashtags`
 * (например, ошибка параметров: `{"errors": {...}}`).
 */
fun parseXLiveTagPage(json: String): XLiveTagPage? {
    val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return null
    val hashtags = root["hashtags"] as? JsonArray ?: return null
    return XLiveTagPage(
        tags = hashtags.mapNotNull { (it as? JsonObject)?.toTag() },
        total = root.primitive("total")?.intOrNull ?: 0,
    )
}

private fun JsonObject.toTag(): XLiveTag? {
    val name = string("hashtag")?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return XLiveTag(
        name = name,
        viewers = primitive("viewer_count")?.intOrNull ?: 0,
        rooms = primitive("room_count")?.intOrNull ?: 0,
        topRooms = (this["top_rooms"] as? JsonArray)
            ?.mapNotNull { room ->
                val obj = room as? JsonObject ?: return@mapNotNull null
                val username = obj.string("room")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                XLiveTagRoom(username = username, thumbnail = obj.string("img").orEmpty())
            }
            .orEmpty(),
    )
}
