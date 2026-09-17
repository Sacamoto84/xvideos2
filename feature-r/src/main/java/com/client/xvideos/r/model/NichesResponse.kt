package com.client.xvideos.r.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class NichesResponse(
    @SerialName("niches") val niches: List<Niche> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("pages") val pages: Int = 0,
    @SerialName("total") val total: Int = 0
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
@Immutable
@Serializable
data class Niche(
    @SerialName("id") val id: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("gifs") val gifs: Long = 0L,
    @SerialName("subscribers") val subscribers: Long = 0L,
    @SerialName("thumbnail") val thumbnail: String = "",
    @SerialName("previews") val previews: List<Preview>? = null
)

@Immutable
@Serializable
data class Preview(
    @SerialName("id") val id: String = "",
    @SerialName("thumbnail") val thumbnail: String = ""
)






