package com.client.xvideos.l.model

/**
 * Альбом в избранном на сервере: статус лайка задан, и это не «нет» и не
 * «дизлайк». Правило одно для первичной синхронизации и для переключения.
 */
fun String?.isLFavoriteLikeStatus(): Boolean = !isNullOrBlank() && this != "none" && this != "dislike"
