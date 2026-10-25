package com.client.xvideos.xlive.network

import io.ktor.http.encodeURLPathPart

/** Начало адреса снимка эфира: в списке комнат снимок приходит как `<начало>/<ник>.jpg?<время>`. */
private const val SNAPSHOT_URL_START = "https://thumb.live.mmcdn.com/riw"

/** Шаг метки времени в адресе снимка, секунды. */
private const val SNAPSHOT_STEP_SECONDS = 60L

/**
 * Снимок эфира комнаты [username] — тот же адрес, что приходит в списке
 * комнат, но собранный по нику: у «Моих» и «Истории» списка нет.
 *
 * Метка [nowSeconds] округляется до минуты: в пределах минуты адрес один, и
 * снимок берётся из кеша картинок, а новая метка даёт свежий кадр.
 */
fun xLiveSnapshotUrl(username: String, nowSeconds: Long): String {
    val stamp = nowSeconds / SNAPSHOT_STEP_SECONDS * SNAPSHOT_STEP_SECONDS
    return "$SNAPSHOT_URL_START/${username.encodeURLPathPart()}.jpg?$stamp"
}
