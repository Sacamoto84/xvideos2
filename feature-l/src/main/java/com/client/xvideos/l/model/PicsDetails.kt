package com.client.xvideos.l.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Детальная карточка изображения (или анимированного видео) в альбоме Luscious.
 *
 * @Immutable — обещание Compose, что объект после создания не меняется.
 *
 * Без него отчёт компилятора помечает класс как `Uncertain(List)`: поле
 * [thumbnails] имеет тип `List`, а это интерфейс, за которым мог бы прятаться
 * изменяемый список. Из-за одного этого поля весь класс переставал быть
 * статически стабильным, и каждое сравнение элемента сетки уходило в проверку
 * стабильности на рантайме.
 *
 * Обещание правдиво: все поля `val`, список приходит из JSON и нигде не
 * мутируется. Если кто-то соберётся его менять — сначала снять аннотацию.
 *
 * @property height Высота исходного изображения.
 * @property width Ширина исходного изображения.
 * @property is_animated Флаг анимации (GIF или MP4 видеопоток).
 * @property url_to_original URL к оригинальному файлу полного разрешения.
 * @property url_to_video URL к видео-версии (для анимированных картинок).
 * @property album Имя или ID альбома.
 * @property thumbnails Список миниатюр различных размеров [Thumbnails].
 * @property id Уникальный ID картинки.
 * @property url Относительный веб-URL картинки на сайте.
 */
@Immutable
@Parcelize
@Serializable
@Suppress("ConstructorParameterNaming")
data class PicsDetails(
    @SerialName("height") val height: Int = 0, //"846"
    @SerialName("width") val width: Int = 0, //"1280"
    @SerialName("is_animated") val is_animated: Boolean = false,
    @SerialName("url_to_original") val url_to_original: String? = null,
    @SerialName("url_to_video") val url_to_video: String? = null,
    @SerialName("album") val album: String? = "null",
    @SerialName("thumbnails") val thumbnails: List<Thumbnails>? = emptyList(),
    @SerialName("id") val id: String? = null,
    @SerialName("url") val url: String? = null
) : Parcelable {
    val isValid: Boolean get() = !id.isNullOrBlank()
    val hasValidId: Boolean get() = !id.isNullOrBlank() && id != "0"
    val hasVideo: Boolean get() = !url_to_video.isNullOrBlank()
    val isGifOrVideo: Boolean get() = is_animated || hasVideo
    val hasOriginal: Boolean get() = !url_to_original.isNullOrBlank()
    val hasThumbnails: Boolean get() = !thumbnails.isNullOrEmpty()
    val numericId: Long get() = id?.toLongOrNull() ?: 0L
    val thumbnailsCount: Int get() = thumbnails?.size ?: 0
    val hasDimensions: Boolean get() = width > 0 && height > 0
    val aspectRatio: Float get() = if (height > 0) width.toFloat() / height.toFloat() else 0f

    fun findThumbnailBySizeOrNull(sizeName: String?): Thumbnails? =
        if (sizeName.isNullOrBlank() || thumbnails.isNullOrEmpty()) null
        else thumbnails.firstOrNull { it.size.equals(sizeName, ignoreCase = true) }

    companion object {
        val EMPTY = PicsDetails()
    }
}

/**
 * Вариант миниатюры картинки определенного разрешения.
 *
 * @property width Ширина миниатюры.
 * @property height Высота миниатюры.
 * @property size Название размера (например, `"small"`, `"large_thumbnail"`, `"xMax"`).
 * @property url URL файла миниатюры на CDN.
 */
@Immutable
@Parcelize
@Serializable
data class Thumbnails(
    @SerialName("width") val width: Int = 0, //640,
    @SerialName("height") val height: Int = 0, //3779,
    @SerialName("size") val size: String? = null, //"small", "xMax"
    @SerialName("url") val url: String? = null //"https://..."
) : Parcelable {
    val isValid: Boolean get() = !url.isNullOrBlank()
    val hasUrl: Boolean get() = !url.isNullOrBlank()
    val hasSize: Boolean get() = !size.isNullOrBlank()
    val hasDimensions: Boolean get() = width > 0 && height > 0
    val aspectRatio: Float get() = if (height > 0) width.toFloat() / height.toFloat() else 0f


    companion object {
        val EMPTY = Thumbnails()
    }
}
