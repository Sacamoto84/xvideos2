package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class NicheResponse(
    @SerialName("niche") val niche: NichesInfo
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
@Immutable
@Serializable
data class NichesInfo(
    @SerialName("cover") val cover: String? = "cover",           //Большая широкая картинка
    @SerialName("description") val description: String = "description",
    @SerialName("gifs") val gifs: Long = -1,
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("owner") val owner: String = "owner",
    @SerialName("subscribers") val subscribers: Long = -1,
    @SerialName("thumbnail") val thumbnail: String = "thumbnail", //200x200 картинка
    @SerialName("rules") val rules: String? = "rules",
)
