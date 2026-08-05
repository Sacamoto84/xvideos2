package com.client.xvideos.r.model

import com.google.gson.annotations.SerializedName



data class NichesResponse(
    @SerializedName("niches") val niches: List<Niche>,
    @SerializedName("page") val page: Int,
    @SerializedName("pages") val pages: Int,
    @SerializedName("total") val total: Int
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
data class Niche(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("gifs") val gifs: Long,
    @SerializedName("subscribers") val subscribers: Long,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("previews") val previews: List<Preview>?
)

data class Preview(
    @SerializedName("id") val id: String,
    @SerializedName("thumbnail") val thumbnail: String
)






