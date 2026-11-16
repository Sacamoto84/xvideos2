package com.client.xvideos.n.model

import kotlinx.serialization.Serializable

/**
 * Отдельная сохранённая страница/картинка из галереи манги nhentai.
 *
 * @property id Уникальный ключ картинки: `"${galleryId}_${pageNumber}"`.
 * @property galleryId Идентификатор галереи манги.
 * @property mediaId Идентификатор хранилища медиа на CDN.
 * @property pageNumber Номер страницы в манге.
 * @property galleryTitle Название манги, из которой сохранена картинка.
 * @property ext Расширение файла ("jpg", "png", "webp", "gif").
 * @property width Ширина изображения в пикселях.
 * @property height Высота изображения в пикселях.
 * @property path Относительный путь CDN к полному файлу (API v2).
 * @property thumbnailPath Относительный путь CDN к миниатюре (API v2).
 * @property addedAt Время добавления в миллисекундах.
 */
@Serializable
data class NPicture(
    val id: String = "",
    val galleryId: Long = 0L,
    val mediaId: Long = 0L,
    val pageNumber: Int = 1,
    val galleryTitle: String = "",
    val ext: String = "jpg",
    val width: Int = 0,
    val height: Int = 0,
    val path: String = "",
    val thumbnailPath: String = "",
    val addedAt: Long = System.currentTimeMillis()
) : java.io.Serializable {

    val imageUrl: String
        get() = when {
            path.isNotEmpty() -> "https://i.nhentai.net/$path"
            else -> "https://i.nhentai.net/galleries/$mediaId/$pageNumber.$ext"
        }

    val thumbnailUrl: String
        get() = when {
            thumbnailPath.isNotEmpty() -> "https://t.nhentai.net/$thumbnailPath"
            else -> "https://t.nhentai.net/galleries/$mediaId/${pageNumber}t.$ext"
        }

    companion object {
        fun makeId(galleryId: Long, pageNumber: Int): String = "${galleryId}_$pageNumber"

        fun fromPage(
            galleryId: Long,
            mediaId: Long,
            galleryTitle: String,
            page: NPage
        ): NPicture = NPicture(
            id = makeId(galleryId, page.pageNumber),
            galleryId = galleryId,
            mediaId = mediaId,
            pageNumber = page.pageNumber,
            galleryTitle = galleryTitle,
            ext = page.ext,
            width = page.width,
            height = page.height,
            path = page.path,
            thumbnailPath = page.thumbnailPath
        )
    }
}
