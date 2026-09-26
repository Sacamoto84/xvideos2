package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import java.io.Serializable

/** `Serializable` вслед за [GifsInfo], который её держит. */
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
    val hasThumbnail: Boolean get() = thumbnail.isNotBlank()
    val hasSd: Boolean get() = sd.isNotBlank()
    val hasHd: Boolean get() = !hd.isNullOrBlank()
    val hasSilent: Boolean get() = !silent.isNullOrBlank()
    val hasPoster: Boolean get() = !poster.isNullOrBlank()
    val hasHtml: Boolean get() = !html.isNullOrBlank()
    val bestVideoUrl: String get() = hd?.takeIf { it.isNotBlank() } ?: sd
    val bestImageUrl: String get() = poster?.takeIf { it.isNotBlank() } ?: thumbnail
    val isValid: Boolean get() = thumbnail.isNotBlank() || sd.isNotBlank()

    companion object {
        val EMPTY = URL1()
    }
}

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
