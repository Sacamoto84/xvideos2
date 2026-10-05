package com.client.xvideos.l.model

/**
 * id альбома, если он известен. Старые сохранённые данные и модель по умолчанию
 * обозначают его отсутствие строкой `"null"` — это правило знает только эта функция.
 */
fun String?.asLAlbumIdOrNull(): String? = this?.takeIf { it.isNotBlank() && it != "null" }

/** id альбома картинки, если он известен — см. [asLAlbumIdOrNull]. */
val PicsDetails.albumIdOrNull: String? get() = album.asLAlbumIdOrNull()
