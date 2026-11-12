package com.client.xvideos.n.model

import kotlinx.serialization.Serializable

/**
 * Облегчённое превью галереи для отображения в списках и сетках.
 *
 * @property id Идентификатор галереи.
 * @property mediaId Идентификатор медиа-хранилища на CDN.
 * @property title Название для отображения в карточке.
 * @property thumbnailUrl Ссылка на миниатюру обложки.
 * @property numPages Количество страниц.
 * @property language Язык (если распознан).
 */
@Serializable
data class NGalleryPreview(
    val id: Long = 0L,
    val mediaId: Long = 0L,
    val title: String = "",
    val thumbnailUrl: String = "",
    val numPages: Int = 0,
    val language: String = ""
) : java.io.Serializable
