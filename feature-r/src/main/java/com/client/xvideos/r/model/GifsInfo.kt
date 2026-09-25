package com.client.xvideos.r.model

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import java.io.Serializable

/**
 * `Serializable` обязателен: модель лежит в `ScreenRedFullScreen`, а экраны
 * Voyager (`Screen : Serializable`) целиком уходят в saved state активити через
 * `Parcel.writeSerializable`. Без этого приложение падает с
 * `NotSerializableException`, когда система сохраняет состояние.
 * Тот же приём уже применён к `P2pSendSource`.
 */
@Stable
@kotlinx.serialization.Serializable
data class GifsInfo(
    @SerialName("id") val id: String = "",
    @SerialName("createDate") val createDate: Long = 0,
    @SerialName("contentType") val contentType: String = "Solo Female",
    @SerialName("likes") val likes: Int = 0,
    @SerialName("width") val width: Int = 100,
    @SerialName("height") val height: Int = 100,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("description") val description: String = "Описание",
    @SerialName("views") val views: Long? = null,
    @SerialName("type") val type: Int = 0,  //1-Gif 2-Image
    @SerialName("userName") val userName: String = "userName",           // "lilijunex"
    @SerialName("urls") val urls: URL1 = URL1(),
    @SerialName("duration") val duration: Double? = null, //15.033,
    @SerialName("hls") val hls: Boolean? = null,
    @SerialName("niches") val niches: List<String>? = null,
) : Serializable

fun GifsInfo.sanitizeOrNull(): GifsInfo? {
    val safeId: String? = id
    if (safeId.isNullOrBlank()) return null

    val safeContentType: String? = contentType
    val safeTags: List<String>? = tags
    val safeDescription: String? = description
    val safeUserName: String? = userName
    val safeUrls: URL1? = urls

    val sanitizedTags = sanitizeTagsList(safeTags)
    val sanitizedUrls = safeUrls?.sanitize() ?: URL1()

    val stringsValid = safeContentType != null && safeDescription != null && safeUserName != null
    if (stringsValid && sanitizedTags === safeTags && sanitizedUrls === safeUrls) {
        return this
    }

    return copy(
        id = safeId,
        contentType = safeContentType ?: "Solo Female",
        tags = sanitizedTags,
        description = safeDescription.orEmpty(),
        userName = safeUserName.orEmpty(),
        urls = sanitizedUrls
    )
}

private fun sanitizeTagsList(safeTags: List<String>?): List<String> {
    if (safeTags == null || safeTags.isEmpty()) return emptyList()
    var hasInvalid = false
    for (tag in safeTags) {
        val s: String? = tag
        if (s.isNullOrBlank()) {
            hasInvalid = true
            break
        }
    }
    if (!hasInvalid) return safeTags
    val out = ArrayList<String>(safeTags.size)
    for (tag in safeTags) {
        val s: String? = tag
        if (!s.isNullOrBlank()) {
            out.add(s)
        }
    }
    return out
}

fun List<GifsInfo>?.sanitizeGifsInfoList(): List<GifsInfo> {
    if (this.isNullOrEmpty()) return emptyList()
    val seenIds = HashSet<String>(this.size)
    val result = ArrayList<GifsInfo>(this.size)
    for (item in this) {
        val safeItem: GifsInfo? = item
        val sanitized = safeItem?.sanitizeOrNull() ?: continue
        if (seenIds.add(sanitized.id)) {
            result.add(sanitized)
        }
    }
    return result
}
