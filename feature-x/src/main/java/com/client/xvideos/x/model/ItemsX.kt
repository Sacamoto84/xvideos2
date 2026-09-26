package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable
import java.io.Serializable

/**
 * Значения по умолчанию обязаны быть **у всех** полей, и это не косметика.
 *
 * Избранное хранится файлами и читается через kotlinx.serialization.
 * `Serializable` обязателен: модель передаётся в экраны Voyager (`ScreenX_VideoPlayer`,
 * `ScreenX_LocalVideoPlayer`), которые уходят в saved state через Java-сериализацию.
 */
@Immutable
@kotlinx.serialization.Serializable
data class ItemsX(
    val id : Long = 0L,               //   Номер 234234233 берется из сайта
    val title : String = "",          // - Название видео(Зависит от выбранного языка)
    val duration : String = "",       // * Длинна видео (11 мин.)
    val views : String = "",          // - Количество просмотров
    val channel : String = "",        // * Отображаемое название канала (Old4k)
    val previewImage : String = "",   // * Путь до картинки превью
    val previewVideo : String = "",   // * Путь до видео превью
    val href: String = "",            // * Путь до страницы видео (Для открытия в экране плеера) Только оно и нужно для этого
    val nameProfile: String = "",     // - Отображаемое название профиля
    val linkProfile: String = "",     // * Путь до профиля путь к каналу (/old4k)
) : Serializable {
    val isValid: Boolean get() = id > 0L
    val isEmpty: Boolean get() = id <= 0L
    val isNotEmpty: Boolean get() = id > 0L
    val hasVideoPreview: Boolean get() = previewVideo.isNotBlank()
    val hasImagePreview: Boolean get() = previewImage.isNotBlank()
    val hasProfile: Boolean get() = nameProfile.isNotBlank() || linkProfile.isNotBlank()
    val hasChannel: Boolean get() = channel.isNotBlank()

    companion object {
        val EMPTY = ItemsX()
    }
}
