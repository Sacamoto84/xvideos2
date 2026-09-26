package com.client.xvideos.l.model

import androidx.compose.runtime.Immutable
import java.io.Serializable
import java.util.UUID

/**
 * Сохраненный пресет фильтрации списка альбомов.
 *
 * Позволяет пользователю сохранять часто используемые комбинации фильтров
 * (жанры, сортировка, теги, диапазоны страниц/рейтинга) под произвольным именем
 * для быстрого применения в будущем.
 *
 * @property id Уникальный идентификатор сохраненного пресета (UUID).
 * @property name Пользовательское название пресета.
 * @property createdAt Временная метка создания пресета в миллисекундах (UTC).
 * @property filter Сконфигурированный объект фильтрации [AlbumListFilter].
 */
@Immutable
@kotlinx.serialization.Serializable
data class SavedAlbumFilter(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val filter: AlbumListFilter = AlbumListFilter.DEFAULT
) : Serializable {
    /** Флаг валидности пресета (имя не должно быть пустым). */
    val isValid: Boolean get() = name.isNotBlank()

    /** Флаг отсутствия имени у пресета. */
    val isEmpty: Boolean get() = name.isBlank()

    /** Флаг наличия непустого имени у пресета. */
    val isNotEmpty: Boolean get() = name.isNotBlank()

    /** Флаг наличия активных условий фильтрации (отличается ли от настроек по умолчанию). */
    val hasFilter: Boolean get() = filter != AlbumListFilter.DEFAULT

    companion object {
        /** Пустой шаблон пресета фильтра без имени и со стандартными настройками. */
        val EMPTY = SavedAlbumFilter(name = "")
    }
}
