package com.client.xvideos.n.model

import kotlinx.serialization.Serializable

/**
 * Тег манги / галереи nhentai.
 *
 * @property id Уникальный идентификатор тега.
 * @property type Категория тега: artist, character, parody, tag, group, language, category.
 * @property name Отображаемое название тега.
 * @property count Количество материалов с этим тегом.
 * @property url Относительный путь URL тега на сайте.
 */
@Serializable
data class NTag(
    val id: Long = 0L,
    val type: String = "",
    val name: String = "",
    val count: Int = 0,
    val url: String = ""
) : java.io.Serializable
