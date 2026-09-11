package com.client.xvideos.r.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NichesResponse(
    @SerializedName("niches") @SerialName("niches") val niches: List<Niche> = emptyList(),
    @SerializedName("page") @SerialName("page") val page: Int = 0,
    @SerializedName("pages") @SerialName("pages") val pages: Int = 0,
    @SerializedName("total") @SerialName("total") val total: Int = 0
)

/**
 * ```kotlin
 *   "id": "female-backs",
 *   "name": "Female Backs",
 *   "gifs": 245,
 *   "subscribers": 914,
 *   "thumbnail": "https://userpic.redgifs.com/niches/thumbnails/female-backs-dee7838f.jpg",
 *   previews": [
 *                 {
 *                     "id": "dangerouswanmice",
 *                     "thumbnail": "https://media.redgifs.com/DangerousWanMice-mobile.jpg"
 *                 },
 *                 {
 *                     "id": "weirddaringbovine",
 *                     "thumbnail": "https://media.redgifs.com/WeirdDaringBovine-mobile.jpg"
 *                 },
 *                 {
 *                     "id": "unsteadyphonywren",
 *                     "thumbnail": "https://media.redgifs.com/UnsteadyPhonyWren-mobile.jpg"
 *                 }
 *             ]
 * ```
 */
@Serializable
data class Niche(
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("name") @SerialName("name") val name: String = "",
    @SerializedName("gifs") @SerialName("gifs") val gifs: Long = 0L,
    @SerializedName("subscribers") @SerialName("subscribers") val subscribers: Long = 0L,
    @SerializedName("thumbnail") @SerialName("thumbnail") val thumbnail: String = "",
    @SerializedName("previews") @SerialName("previews") val previews: List<Preview>? = null
)

@Serializable
data class Preview(
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("thumbnail") @SerialName("thumbnail") val thumbnail: String = ""
)






