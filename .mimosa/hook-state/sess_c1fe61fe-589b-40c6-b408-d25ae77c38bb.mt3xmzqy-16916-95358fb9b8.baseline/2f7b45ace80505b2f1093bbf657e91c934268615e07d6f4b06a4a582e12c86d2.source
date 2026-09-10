package com.client.xvideos.r.model

import com.google.gson.annotations.SerializedName

data class NicheResponse(
    @SerializedName("niche") val niche: NichesInfo
)

/**
 * ```json
 * "niches": [
 *     {
 *       "cover": "https://userpic.redgifs.com/niches/covers/big-areolas.jpg",
 *       "description": "NSFW GIFs and images featuring women with large areolas.",
 *       "gifs": 29209,
 *       "id": "big-areolas",
 *       "name": "Big Areolas",
 *       "owner": "phpunit",
 *       "subscribers": 77917,
 *       "thumbnail": "https://userpic.redgifs.com/niches/thumbnails/big-areolas.jpg",
 *       "rules": "1. Big Areolas 2. Porn featuring females with large areolas. 3. Are title and description relevant to the gif?"
 *     },
 *     {
 *       "cover": "https://userpic.redgifs.com/niches/covers/legal-teens.jpg",
 *       "description": "NSFW GIFs and images featuring 18 or 19 year old women.",
 *       "gifs": 654498,
 *       "id": "legal-teens",
 *       "name": "Legal Teens",
 *       "owner": "phpunit",
 *       "subscribers": 476841,
 *       "thumbnail": "https://userpic.redgifs.com/niches/thumbnails/legal-teens.jpg",
 *       "rules": "1. Legal Teens 2. Porn featuring legal aged, female teens. 3. Are title and description relevant to the gif?"
 *     },
 *     ```
 */
data class NichesInfo(
    @SerializedName("cover") val cover: String? = "cover",           //Большая широкая картинка
    @SerializedName("description") val description: String = "description",
    @SerializedName("gifs") val gifs: Long = -1,
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("owner") val owner: String = "owner",
    @SerializedName("subscribers") val subscribers: Long = -1,
    @SerializedName("thumbnail") val thumbnail: String = "thumbnail", //200x200 картинка
    @SerializedName("rules") val rules: String? = "rules",
)
