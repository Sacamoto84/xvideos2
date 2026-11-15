package com.client.xvideos.n.model

import kotlinx.serialization.Serializable

/**
 * Полная информация о галерее / манге nhentai.
 *
 * @property id Идентификатор галереи.
 * @property mediaId Идентификатор медиа-хранилища на CDN.
 * @property title Основное название (pretty / english).
 * @property englishTitle Английское название.
 * @property japaneseTitle Японское название.
 * @property uploadDate Время загрузки в Unix timestamp (секунды).
 * @property numPages Количество страниц.
 * @property numFavorites Количество добавлений в избранное на сайте.
 * @property cover Обложка галереи.
 * @property thumbnail Миниатюра галереи.
 * @property tags Список тегов по категориям.
 * @property pages Список страниц с метаданными размеров и расширений.
 */
@Serializable
data class NGallery(
    val id: Long = 0L,
    val mediaId: Long = 0L,
    val title: String = "",
    val englishTitle: String = "",
    val japaneseTitle: String = "",
    val uploadDate: Long = 0L,
    val numPages: Int = 0,
    val numFavorites: Int = 0,
    val cover: NPage = NPage(),
    val thumbnail: NPage = NPage(),
    val tags: List<NTag> = emptyList(),
    val pages: List<NPage> = emptyList()
) : java.io.Serializable {
    /**
     * Ссылка на обложку галереи.
     */
    val coverUrl: String
        get() = when {
            cover.path.isNotEmpty() -> "https://t.nhentai.net/${cover.path}"
            else -> "https://t.nhentai.net/galleries/$mediaId/cover.${cover.ext}"
        }

    /**
     * Ссылка на миниатюру обложки галереи.
     */
    val thumbnailUrl: String
        get() = when {
            thumbnail.path.isNotEmpty() -> "https://t.nhentai.net/${thumbnail.path}"
            else -> "https://t.nhentai.net/galleries/$mediaId/thumb.${thumbnail.ext}"
        }

    /**
     * Список художников (artist).
     */
    val artists: List<NTag>
        get() = tags.filter { it.type.equals("artist", ignoreCase = true) }

    /**
     * Список категорий/жанров (tag).
     */
    val generalTags: List<NTag>
        get() = tags.filter { it.type.equals("tag", ignoreCase = true) }

    /**
     * Список первоисточников (parody).
     */
    val parodies: List<NTag>
        get() = tags.filter { it.type.equals("parody", ignoreCase = true) }

    /**
     * Список персонажей (character).
     */
    val characters: List<NTag>
        get() = tags.filter { it.type.equals("character", ignoreCase = true) }

    /**
     * Список языков.
     */
    val languages: List<NTag>
        get() = tags.filter { it.type.equals("language", ignoreCase = true) && !it.name.equals("translated", ignoreCase = true) }
}
