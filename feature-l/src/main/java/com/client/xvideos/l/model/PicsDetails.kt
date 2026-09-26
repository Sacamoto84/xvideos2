package com.client.xvideos.l.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
```json
{
    "__typename": "Picture",
    "id": "59362744",
    "title": "Millie Beachside Demon Latex Seductress By Frwds Dleksko Fullview",
    "description": "",
    "created": 1770995944.280297,
    "like_status": "none",                     // !Не используем
    "number_of_comments": 0,                   // !Не используем
    "number_of_favorites": 0,                  // !Не используем
    "moderation_status": "NOT_MODERATED",      // !Не используем
    "width": 1920,                             //
    "height": 2803,                            //
    "resolution": "1920x2803",                 //
    "aspect_ratio": "1920:2803",               //
    "url_to_original": null,                   // !Не используем
    "url_to_video": null,                      // Адрес видео
    "is_animated": false,
    "position": 42,
    "permissions": [ "create" ],               // !Не используем
    "url": "/pictures/album/millie_603323/id/59362744/@millie_beachside_demon_latex_seductress_by_frwds_d",
    "tags": [],
    "thumbnails": [
    {
        "width": 1680,
        "target_width": 1600,
        "height": 2453,
        "size": "xMax",
        "url": "https://cdni.luscious.net/venividivici2k13/603323/millie_beachside_dem_01KHBSB2THB9YFJCQT22P9NGCS.1680x0.jpg?md5=fgYNqcEoz8zeAuRLpEaUGQ&expires=1773900756"
    },
    {
        "width": 640,
        "target_width": 400,
        "height": 935,
        "size": "small",
        "url": "https://cdni.luscious.net/venividivici2k13/603323/millie_beachside_dem_01KHBSB2THB9YFJCQT22P9NGCS.640x0.jpg?md5=sn0bj1zYPF7ziGsGKnGRQA&expires=1773900756"
    },
    {
        "width": 315,
        "target_width": 0,
        "height": 460,
        "size": "large_thumbnail",
        "url": "https://cdni.luscious.net/venividivici2k13/603323/millie_beachside_dem_01KHBSB2THB9YFJCQT22P9NGCS.315x0.jpg?md5=8itQfJ2Q00VR9BWCUlvXRA&expires=1773900756"
    }
    ]
}
```
*/
/**
 * @Immutable — обещание Compose, что объект после создания не меняется.
 *
 * Без него отчёт компилятора помечает класс как `Uncertain(List)`: поле
 * [thumbnails] имеет тип `List`, а это интерфейс, за которым мог бы прятаться
 * изменяемый список. Из-за одного этого поля весь класс переставал быть
 * статически стабильным, и каждое сравнение элемента сетки уходило в проверку
 * стабильности на рантайме.
 *
 * Обещание правдиво: все поля `val`, список приходит из JSON и нигде не
 * мутируется. Если кто-то соберётся его менять — сначала снять аннотацию.
 */
@Immutable
@Parcelize
@Serializable
@Suppress("ConstructorParameterNaming")
data class PicsDetails(
    @SerialName("height") val height: Int = 0, //"846"
    @SerialName("width") val width: Int = 0, //"1280"
    @SerialName("is_animated") val is_animated: Boolean = false,
    @SerialName("url_to_original") val url_to_original: String? = null,
    @SerialName("url_to_video") val url_to_video: String? = null,
    @SerialName("album") val album: String? = "null",
    @SerialName("thumbnails") val thumbnails: List<Thumbnails>? = emptyList(),
    @SerialName("id") val id: String? = null,
    @SerialName("url") val url: String? = null
) : Parcelable {
    val isValid: Boolean get() = !id.isNullOrBlank()
    val hasVideo: Boolean get() = !url_to_video.isNullOrBlank()
    val hasOriginal: Boolean get() = !url_to_original.isNullOrBlank()
    val hasThumbnails: Boolean get() = !thumbnails.isNullOrEmpty()

    companion object {
        val EMPTY = PicsDetails()
    }
}

@Immutable
@Parcelize
@Serializable
data class Thumbnails(
    @SerialName("width") val width: Int = 0, //640,
    @SerialName("height") val height: Int = 0, //3779,
    @SerialName("size") val size: String? = null, //"small", "xMax"
    @SerialName("url") val url: String? = null //"https://..."
) : Parcelable {
    val isValid: Boolean get() = !url.isNullOrBlank()

    companion object {
        val EMPTY = Thumbnails()
    }
}

//https://cdni.luscious.net/venividivici2k13/603323/millie_beachside_dem_01KHBSB2THB9YFJCQT22P9NGCS.640x0.jpg?md5=sn0bj1zYPF7ziGsGKnGRQA&expires=1773900756
//[
//{
//    "width": 1680,
//    "height": 2044,
//    "size": "xMax",
//    "url": "https://ah-img.luscious.net/Senred/554756/img_20250420_143600_01K3KQKZAWZVQB2X2RQXE7CWDC.1680x0.jpg"
//},
//{
//    "width": 640,
//    "height": 779,
//    "size": "small",
//    "url": "https://ah-img.luscious.net/Senred/554756/img_20250420_143600_01K3KQKZAWZVQB2X2RQXE7CWDC.640x0.jpg"
//},
//{
//    "width": 315,
//    "height": 384,
//    "size": "large_thumbnail",
//    "url": "https://ah-img.luscious.net/Senred/554756/img_20250420_143600_01K3KQKZAWZVQB2X2RQXE7CWDC.315x0.jpg"
//}
//]
