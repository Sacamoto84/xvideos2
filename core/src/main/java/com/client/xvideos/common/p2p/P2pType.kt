package com.client.xvideos.common.p2p

/**
 * Источник item: xvideos, redgifs, luscious. Определяет store на приёмной стороне.
 * [L_ALBUM] — метаданные альбома L (`<id>.album`), контент получатель качает сам.
 * [L_COLLECTION] — коллекция L одним zip-архивом (реальные файлы).
 */
enum class P2pType { X, R, L, L_ALBUM, L_COLLECTION, R_COLLECTION }
