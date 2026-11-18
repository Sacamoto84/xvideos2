package com.client.xvideos.n.model

import kotlinx.serialization.Serializable

/**
 * Результат постраничной выборки галерей манги.
 *
 * @property items Список найденных галерей на текущей странице.
 * @property page Номер текущей страницы (начиная с 1).
 * @property totalPages Общее количество доступных страниц.
 */
@Serializable
data class NGalleryPageResult(
    val items: List<NGalleryPreview> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1
) : java.io.Serializable
