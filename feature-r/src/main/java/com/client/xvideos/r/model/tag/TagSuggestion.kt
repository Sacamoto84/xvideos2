package com.client.xvideos.r.model.tag

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class TagSuggestion(
    @SerialName("gifs") val gifs: Long = 0L,
    @SerialName("text") val text: String = "",
    @SerialName("type") val type: String = ""
) {
    val isValid: Boolean get() = text.isNotBlank()
    val hasGifs: Boolean get() = gifs > 0L
    val hasType: Boolean get() = type.isNotBlank()

    companion object {
        val EMPTY = TagSuggestion()
    }
}
