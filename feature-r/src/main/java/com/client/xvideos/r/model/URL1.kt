package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import java.io.Serializable

/**
 * Модель URL-адресов видеофайлов, превью и постеров для конкретного медиаобъекта RedGifs.
 *
 * Предоставляет ссылки на MP4 в различном качестве (SD, HD, Silent), а также изображения (thumbnail, poster).
 * Реализует [Serializable] вслед за [GifsInfo], в котором содержится.
 *
 * @property thumbnail URL уменьшенного изображения превью (обычно мобильного размера).
 * @property silent URL видеофайла высокого разрешения (HD) без аудиодорожки.
 * @property poster URL полноразмерного постера/обложки видео.
 * @property html URL веб-страницы плеера (iframe/embed).
 * @property sd URL видеофайла стандартного разрешения (SD, мобильное качество).
 * @property hd URL видеофайла высокого разрешения со звуком (может отсутствовать у части роликов).
 */
@Immutable
@kotlinx.serialization.Serializable
data class URL1(
    @SerialName("thumbnail") val thumbnail: String = "",     // Картинка как SD
    @SerialName("silent") val silent: String? = null,     // * Полное видео в mp4 !!! Без звука в HD Для скачивания
    @SerialName("poster") val poster: String? = null,     // Большая картинка Видео как HD
    @SerialName("html") val html: String? = null,       // * Ссылка на веб-страницу с медиа. Полноэкранный режим. Типа ссылки
    @SerialName("sd") val sd: String = "",            // * SD-ссылка на медиафайл.                                 3.5 MB
    @SerialName("hd") val hd: String? = null,         // * HD-ссылка на медиафайл (может отсутствовать). Со звуком 21MB
) : Serializable {

    /** Проверяет наличие непустого thumbnail URL. */
    val hasThumbnail: Boolean get() = thumbnail.isNotBlank()

    /** Проверяет наличие непустого SD URL. */
    val hasSd: Boolean get() = sd.isNotBlank()

    /** Проверяет наличие непустого HD URL. */
    val hasHd: Boolean get() = !hd.isNullOrBlank()

    /** Проверяет наличие ссылки на видео без звука. */
    val hasSilent: Boolean get() = !silent.isNullOrBlank()

    /** Проверяет наличие постера. */
    val hasPoster: Boolean get() = !poster.isNullOrBlank()

    /** Проверяет наличие HTML ссылки плеера. */
    val hasHtml: Boolean get() = !html.isNullOrBlank()

    /**
     * Выбирает наилучший доступный URL видео для воспроизведения или скачивания:
     * отдает [hd], если он присутствует и не пуст, иначе [sd].
     */
    val bestVideoUrl: String get() = hd?.takeIf { it.isNotBlank() } ?: sd

    /**
     * Выбирает наилучший доступный URL картинки для превью:
     * отдает [poster], если он присутствует и не пуст, иначе [thumbnail].
     */
    val bestImageUrl: String get() = poster?.takeIf { it.isNotBlank() } ?: thumbnail

    /** Проверяет наличие хотя бы одного доступного видеофайла. */
    val hasAnyVideoUrl: Boolean get() = hasHd || hasSd || hasSilent

    /** Проверяет наличие хотя бы одного доступного изображения. */
    val hasAnyImageUrl: Boolean get() = hasPoster || hasThumbnail

    /**
     * Выбирает наилучший доступный URL для фонового скачивания файла:
     * отдает [hd], затем [silent], иначе [sd].
     */
    val bestDownloadUrl: String get() = hd?.takeIf { it.isNotBlank() } ?: silent?.takeIf { it.isNotBlank() } ?: sd

    /**
     * Проверяет, содержится ли искомая подстрока URL в каком-либо из адресов объекта.
     */
    fun containsUrl(urlQuery: String?): Boolean {
        if (urlQuery.isNullOrBlank()) return false
        return thumbnail.contains(urlQuery, ignoreCase = true) ||
            (silent?.contains(urlQuery, ignoreCase = true) == true) ||
            (poster?.contains(urlQuery, ignoreCase = true) == true) ||
            (html?.contains(urlQuery, ignoreCase = true) == true) ||
            sd.contains(urlQuery, ignoreCase = true) ||
            (hd?.contains(urlQuery, ignoreCase = true) == true)
    }

    /** Проверяет валидность ссылок (наличие хотя бы thumbnail или sd). */
    val isValid: Boolean get() = thumbnail.isNotBlank() || sd.isNotBlank()

    /** Проверяет наличие валидного URL для воспроизведения видео. */
    val hasValidVideo: Boolean get() = bestVideoUrl.isNotBlank()

    /** Проверяет наличие валидного URL для картинки превью. */
    val hasValidImage: Boolean get() = bestImageUrl.isNotBlank()

    companion object {
        /** Пустой экземпляр [URL1] со значениями по умолчанию. */
        val EMPTY = URL1()
    }
}

/**
 * Очищает и нормализует экземпляр [URL1]:
 * гарантирует, что обязательные строки не будут null в рантайме.
 */
fun URL1.sanitize(): URL1 {
    if (this == URL1.EMPTY) return this
    val safeThumbnail: String? = thumbnail
    val safeSd: String? = sd
    if (safeThumbnail != null && safeSd != null) return this

    return copy(
        thumbnail = safeThumbnail.orEmpty(),
        sd = safeSd.orEmpty()
    )
}

//"urls": {
//    "thumbnail": "https://media.redgifs.com/UnusualAttachedHorseshoecrab-mobile.jpg",
//    "silent": "https://media.redgifs.com/UnusualAttachedHorseshoecrab-silent.mp4",
//    "poster": "https://media.redgifs.com/UnusualAttachedHorseshoecrab-poster.jpg",
//    "html": "https://www.redgifs.com/ifr/unusualattachedhorseshoecrab",
//    "hd": "https://media.redgifs.com/UnusualAttachedHorseshoecrab.mp4",
//    "sd": "https://media.redgifs.com/UnusualAttachedHorseshoecrab-mobile.mp4"
//},
