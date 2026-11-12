package com.client.xvideos.n.model

import kotlinx.serialization.Serializable

/**
 * Страница манги в галерее nhentai.
 *
 * @property pageNumber Порядковый номер страницы (начиная с 1).
 * @property ext Расширение файла: "jpg", "png", "gif", "webp".
 * @property width Ширина изображения в пикселях.
 * @property height Высота изображения в пикселях.
 */
@Serializable
data class NPage(
    val pageNumber: Int = 1,
    val ext: String = "jpg",
    val width: Int = 0,
    val height: Int = 0
) : java.io.Serializable {
    /**
     * Прямая ссылка на полноразмерное изображение страницы в CDN.
     */
    fun imageUrl(mediaId: Long): String =
        "https://i.nhentai.net/galleries/$mediaId/$pageNumber.$ext"

    /**
     * Ссылка на миниатюру страницы в CDN.
     */
    fun thumbnailUrl(mediaId: Long): String =
        "https://t.nhentai.net/galleries/$mediaId/${pageNumber}t.$ext"

    companion object {
        /**
         * Преобразует тип изображения из формата API nhentai (j/p/g/w) в расширение файла.
         */
        fun extFromApiType(type: String): String = when (type.lowercase()) {
            "p" -> "png"
            "g" -> "gif"
            "w" -> "webp"
            else -> "jpg"
        }
    }
}
