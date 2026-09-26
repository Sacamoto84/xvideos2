package com.client.xvideos.r.model.tag

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ сетевого запроса полного каталога тегов (`/v1/tags`).
 *
 * @property tags Коллекция всех тегов [TagInfo].
 */
@Immutable
@Serializable
data class TagsResponse(
    @SerialName("tags") val tags: List<TagInfo> = emptyList()
) {
    /** Проверяет, пуст ли каталог тегов. */
    val isEmpty: Boolean get() = tags.isEmpty()

    /** Проверяет наличие тегов в каталоге. */
    val isNotEmpty: Boolean get() = tags.isNotEmpty()

    /** Количество тегов в каталоге. */
    val size: Int get() = tags.size

    /** Количество тегов в каталоге. */
    val count: Int get() = tags.size

    /** Первый тег или null. */
    val firstOrNull: TagInfo? get() = tags.firstOrNull()

    companion object {
        /** Пустой экземпляр ответа. */
        val EMPTY = TagsResponse()
    }
}
