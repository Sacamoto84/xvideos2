package com.client.xvideos.xlive.network

import com.client.xvideos.xlive.model.XLiveFollowedCount
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Подписка из ответа подписки, отписки и `follow/is_following/<ник>/`:
 * `{"following": true, …}`.
 *
 * @return `null`, если ответ не JSON-объект или в нём нет `following`.
 */
fun parseXLiveFollowing(json: String): Boolean? {
    val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return null
    return root.primitive("following")?.booleanOrNull
}

/**
 * Сколько подписок и сколько из них в эфире, из ответа
 * `follow/api/online_followed_rooms/`: `{"online": 3, "total": 12, …}`.
 *
 * @return `null`, если ответ не JSON-объект или в нём нет `total`.
 */
fun parseXLiveFollowedCount(json: String): XLiveFollowedCount? {
    val root = runCatching { Json.parseToJsonElement(json) }.getOrNull() as? JsonObject ?: return null
    val total = root.primitive("total")?.intOrNull ?: return null
    return XLiveFollowedCount(online = root.primitive("online")?.intOrNull ?: 0, total = total)
}
