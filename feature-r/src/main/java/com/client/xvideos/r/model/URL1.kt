package com.client.xvideos.r.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import java.io.Serializable

/** `Serializable` вслед за [GifsInfo], который её держит. */
@kotlinx.serialization.Serializable
data class URL1(
    @SerializedName("thumbnail") @SerialName("thumbnail") val thumbnail: String = "",     // Картинка как SD
    @SerializedName("silent") @SerialName("silent") val silent: String? = null,     // * Полное видео в mp4 !!! Без звука в HD Для скачивания
    @SerializedName("poster") @SerialName("poster") val poster: String? = null,     // Большая картинка Видео как HD
    @SerializedName("html") @SerialName("html") val html: String? = null,       // * Ссылка на веб-страницу с медиа. Полноэкранный режим. Типа ссылки
    @SerializedName("sd") @SerialName("sd") val sd: String = "",            // * SD-ссылка на медиафайл.                                 3.5 MB
    @SerializedName("hd") @SerialName("hd") val hd: String? = null,         // * HD-ссылка на медиафайл (может отсутствовать). Со звуком 21MB
) : Serializable

fun URL1.sanitize(): URL1 {
    val safeThumbnail: String? = thumbnail
    val safeSd: String? = sd

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
