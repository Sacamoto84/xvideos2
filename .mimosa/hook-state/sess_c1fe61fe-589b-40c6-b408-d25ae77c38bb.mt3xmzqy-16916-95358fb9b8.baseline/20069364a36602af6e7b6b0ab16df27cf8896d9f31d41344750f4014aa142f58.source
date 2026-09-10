package com.client.xvideos.common.p2p

import kotlinx.serialization.Serializable

/**
 * Источник item: xvideos, redgifs, luscious. Определяет store на приёмной стороне.
 * [L_ALBUM] — метаданные альбома L (`<id>.album`), контент получатель качает сам.
 * [L_COLLECTION] — коллекция L одним zip-архивом (реальные файлы).
 *
 * `@Serializable`: тип едет в манифесте с чужого устройства и разбирается
 * kotlinx — неизвестное значение из более новой версии приложения должно быть
 * отказом, а не молчаливым null в non-null поле.
 */
@Serializable
enum class P2pType { X, R, L, L_ALBUM, L_COLLECTION, R_COLLECTION }
