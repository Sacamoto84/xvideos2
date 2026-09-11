package com.client.xvideos.r.model

import androidx.compose.runtime.Stable
import com.google.gson.annotations.SerializedName
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
    @SerializedName("id") @SerialName("id") val id: String = "",
    @SerializedName("createDate") @SerialName("createDate") val createDate: Long = 0,
    @SerializedName("contentType") @SerialName("contentType") val contentType: String = "Solo Female",
    @SerializedName("likes") @SerialName("likes") val likes: Int = 0,
    @SerializedName("width") @SerialName("width") val width: Int = 100,
    @SerializedName("height") @SerialName("height") val height: Int = 100,
    @SerializedName("tags") @SerialName("tags") val tags: List<String> = emptyList(),
    @SerializedName("description") @SerialName("description") val description: String = "Описание",
    @SerializedName("views") @SerialName("views") val views: Long? = null,
    @SerializedName("type") @SerialName("type") val type: Int = 0,  //1-Gif 2-Image
    @SerializedName("userName") @SerialName("userName") val userName: String = "userName",           // "lilijunex"
    @SerializedName("urls") @SerialName("urls") val urls: URL1 = URL1(),
    @SerializedName("duration") @SerialName("duration") val duration: Double? = null, //15.033,
    @SerializedName("hls") @SerialName("hls") val hls: Boolean? = null,
    @SerializedName("niches") @SerialName("niches") val niches: List<String>? = null,
) : Serializable

fun GifsInfo.sanitizeOrNull(): GifsInfo? {
    val safeId: String? = id
    if (safeId.isNullOrBlank()) return null

    val safeContentType: String? = contentType
    val safeTags: List<String>? = tags
    val safeDescription: String? = description
    val safeUserName: String? = userName
    val safeUrls: URL1? = urls

    return copy(
        id = safeId,
        contentType = safeContentType ?: "Solo Female",
        tags = safeTags.orEmpty().mapNotNull { tag ->
            val safeTag: String? = tag
            safeTag?.takeIf { it.isNotBlank() }
        },
        description = safeDescription.orEmpty(),
        userName = safeUserName.orEmpty(),
        urls = safeUrls?.sanitize() ?: URL1()
    )
}

fun List<GifsInfo>?.sanitizeGifsInfoList(): List<GifsInfo> {
    return orEmpty()
        .mapNotNull { item ->
            val safeItem: GifsInfo? = item
            safeItem?.sanitizeOrNull()
        }
        .distinctBy { it.id }
}
