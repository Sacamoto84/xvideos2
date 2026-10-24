package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveRoom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * Ники открытых комнат из ответа `api/ts/roomlist/room-list/`, в порядке сайта.
 *
 * Берутся комнаты с `current_show = public` и без пароля: остальные поток
 * анониму не отдают.
 *
 * @return пустой список, если ответ не тот или открытых комнат нет.
 */
fun parseXLivePublicRooms(json: String): List<String> {
    val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return emptyList()
    val rooms = root["rooms"] as? JsonArray ?: return emptyList()
    return rooms.mapNotNull { element ->
        val room = element as? JsonObject ?: return@mapNotNull null
        val username = (room["username"] as? JsonPrimitive)?.contentOrNull
        val show = (room["current_show"] as? JsonPrimitive)?.contentOrNull
        val hasPassword = (room["has_password"] as? JsonPrimitive)?.booleanOrNull == true
        username?.takeIf { it.isNotBlank() && show == XLiveRoom.STATUS_PUBLIC && !hasPassword }
    }
}
