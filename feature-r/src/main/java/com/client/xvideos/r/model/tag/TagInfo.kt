package com.client.xvideos.r.model.tag

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель описания тега из глобального каталога тегов RedGifs.
 *
 * @property name Название тега.
 * @property count Количество материалов с этим тегом.
 */
@Immutable
@Serializable
data class TagInfo(
    @SerialName("name") val name: String = "",
    @SerialName("count") val count: Long = 0L
) {
    /** Проверяет валидность имени тега. */
    val isValid: Boolean get() = name.isNotBlank()

    /** Проверяет непустоту имени тега. */
    val hasName: Boolean get() = name.isNotBlank()

    /** Проверяет наличие связанных материалов. */
    val hasCount: Boolean get() = count > 0L

    /** Нормализованное имя тега в нижнем регистре без лишних пробелов. */
    val normalizedName: String get() = name.trim().lowercase()

    /** Проверяет соответствие тега поисковому запросу без учета регистра. */
    fun matches(query: String?): Boolean =
        if (query.isNullOrBlank()) false else name.contains(query.trim(), ignoreCase = true)

    /** Проверяет соответствие поисковому запросу с возвратом true для пустых запросов. */
    fun matchesQuery(query: String?): Boolean =
        if (query.isNullOrBlank()) true else matches(query)

    /** Проверяет совпадение тегов по имени. */
    fun isSameTag(other: TagInfo?): Boolean =
        other != null && isValid && normalizedName == other.normalizedName

    companion object {
        /** Пустой экземпляр [TagInfo]. */
        val EMPTY = TagInfo()
    }
}
