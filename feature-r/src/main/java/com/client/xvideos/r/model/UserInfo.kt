package com.client.xvideos.r.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ```json
 *   "users": [
 *     {
 *       "creationtime": 1758278234,
 *       "description": "",
 *       "followers": 21193,
 *       "following": 0,
 *       "gifs": 2324,
 *       "name": "relative_rub",
 *       "profileImageUrl": "https://userpic.redgifs.com/8/9a/89ac53e4968b17e3335e7dc57c53b238.png",
 *       "profileUrl": "https://onlyfans.com/vickicandy/trial/z8eqb14059tqhbzx3n0rudymkmsrky6n",
 *       "premium": {
 *         "subscription_outbound_link": null
 *       },
 *       "publishedCollections": 0,
 *       "publishedGifs": 2176,
 *       "socialUrl1": null,
 *       "socialUrl2": null,
 *       "socialUrl3": null,
 *       "socialUrl4": null,
 *       "socialUrl5": "https://reddit.com/user/Relative_Rub_9070/",
 *       "socialUrl6": null,
 *       "socialUrl7": null,
 *       "socialUrl8": null,
 *       "socialUrl9": null,
 *       "socialUrl10": null,
 *       "socialUrl11": null,
 *       "socialUrl12": null,
 *       "socialUrl13": null,
 *       "socialUrl14": null,
 *       "socialUrl15": null,
 *       "socialUrl16": null,
 *       "socialUrl17": null,
 *       "socialUrl18": null,
 *       "studio": false,
 *       "subscription": 0,
 *       "url": "https://www.redgifs.com/users/relative_rub",
 *       "username": "relative_rub",
 *       "verified": true,
 *       "views": 32986108
 *     }
 *   ],
 * ```
 */
@Serializable
data class UserInfo(
    @SerializedName("description")     @SerialName("description")     val description: String? = null,       // * Описание в профиле
    @SerializedName("creationtime")    @SerialName("creationtime")    val creationtime: Long = 0L,           // * Описание пользователя в его профиле.                     > "Collared sub addicted to XL horse dildos"
    @SerializedName("followers")       @SerialName("followers")       val followers: Long = 0,               // * Количество подписчиков пользователя.                      > 68214
    @SerializedName("gifs")            @SerialName("gifs")            val gifs: Long = 0,                    // * Общее количество опубликованных пользователем GIF-файлов. > 439
    @SerializedName("name")            @SerialName("name")            val name: String = "",                 // * Имя пользователя. Большие буквы> "lilijunex"
    @SerializedName("profileImageUrl") @SerialName("profileImageUrl") val profileImageUrl: String? = null,   // * URL-адрес изображения профиля пользователя. > "https://userpic.redgifs.com/4/8c/48cc3668e114f878aafcc6dfd0a3d4f2.png"
    @SerializedName("profileUrl")      @SerialName("profileUrl")      val profileUrl: String = "",           // * URL-адрес профиля пользователя. Это URL, который отображается в профиле, установленном пользователем. Это НЕ URL пользователя на "redgifs.com" >"https://beacons.ai/lilijunex"
    @SerializedName("publishedGifs")   @SerialName("publishedGifs")   val publishedGifs: Long = 0,             // * Количество опубликованных публичных GIF-файлов.       > 421 (Отображается в профиле)
    @SerializedName("url")             @SerialName("url")             val url: String = "",                              // * URL-адрес пользователя на сайте ``redgifs.com``. > "https://www.redgifs.com/users/lilijunex"
    @SerializedName("username")        @SerialName("username")        val username: String = "",                    // * Имя пользователя. маленькие буквы>"lilijunex"
    @SerializedName("verified")        @SerialName("verified")        val verified: Boolean = false,                // *
    @SerializedName("views")           @SerialName("views")           val views: Long  = 0L,                           // * Общее количество просмотров всех опубликованных пользователем GIF. > 123194825
)

