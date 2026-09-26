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

    /** Проверяет наличие связанных материалов. */
    val hasCount: Boolean get() = count > 0L

    companion object {
        /** Пустой экземпляр [TagInfo]. */
        val EMPTY = TagInfo()
    }
}
