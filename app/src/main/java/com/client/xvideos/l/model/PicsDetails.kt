package com.client.xvideos.l.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import com.google.gson.annotations.SerializedName

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
 * Обещание правдиво: все поля `val`, список приходит из Gson и нигде не
 * мутируется. Если кто-то соберётся его менять — сначала снять аннотацию.
 */
@Immutable
@Parcelize
data class PicsDetails(
    @SerializedName("height") val height: Int = 0, //"846"
    @SerializedName("width")  val width: Int = 0, //"1280"
    @SerializedName("is_animated") val is_animated: Boolean = false,
    @SerializedName("url_to_original") val url_to_original: String? = null,
    @SerializedName("url_to_video") val url_to_video: String? = null,
    @SerializedName("album") val album: String? = "null",
    @SerializedName("thumbnails") val thumbnails: List<Thumbnails>? = emptyList()
) : Parcelable

@Parcelize
data class Thumbnails(
    @SerializedName("width") val width: Int = 0,    //640,
    @SerializedName("height") val height: Int = 0,  //3779,
    @SerializedName("size") val size: String? = null,   //"small", "xMax"
    @SerializedName("url") val url: String? = null      //"https://..."
) : Parcelable

/**
 * Thumbnail size configuration enum with display value mapping
 */
enum class ThumbnailsSize(
    val value: String,
    val displayName: String
) {
    XMAX("xMax", "Large"),
    SMALL("small", "Medium"),
    LARGE_THUMBALIST("large_thumbnail", "Small");

    companion object {
        /**
         * Find ThumbnailsSize by its value
         */
        fun fromValue(value: String): ThumbnailsSize? = entries.find { it.value == value }

        /**
         * Find ThumbnailsSize by its display name
         */
        fun fromDisplayName(displayName: String): ThumbnailsSize? = entries.find { it.displayName == displayName }

        /**
         * Get all available display names
         */
        val displayNames: List<String> = entries.map { it.displayName }
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
