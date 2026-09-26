package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable
import java.io.Serializable

/**
 * Базовая модель карточки видеоролика в разделе X.
 *
 * Значения по умолчанию обязаны быть **у всех** полей для устойчивой десериализации.
 *
 * Избранное хранится файлами и читается через kotlinx.serialization.
 * `Serializable` обязателен: модель передаётся в экраны Voyager (`ScreenX_VideoPlayer`,
 * `ScreenX_LocalVideoPlayer`), которые уходят в saved state через Java-сериализацию.
 *
 * @property id Уникальный числовой ID видео (парсится из ссылки или атрибутов).
 * @property title Название видеоролика (локализуется сайтом).
 * @property duration Текстовая длительность видео (например, `"11 мин."`, `"12:34"`).
 * @property views Текстовое количество просмотров (например, `"1.2M"`).
 * @property channel Отображаемое название канала автора.
 * @property previewImage Ссылка на статическую обложку/постер ролика.
 * @property previewVideo Ссылка на короткое видео-превью (micro-MP4).
 * @property href Относительный или абсолютный путь к странице видео.
 * @property nameProfile Отображаемое имя профиля автора.
 * @property linkProfile Ссылка на страницу профиля/канала.
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
    val hasTitle: Boolean get() = title.isNotBlank()
    val hasDuration: Boolean get() = duration.isNotBlank()
    val hasViews: Boolean get() = views.isNotBlank()
    val hasHref: Boolean get() = href.isNotBlank()
    val hasValidHref: Boolean get() = href.isNotBlank()
    val hasValidTitle: Boolean get() = title.isNotBlank()
    val displayNameProfile: String get() = nameProfile.ifBlank { channel }
    val normalizedTitle: String get() = title.trim()
    val normalizedChannel: String get() = channel.trim()
    val cleanProfileLink: String get() = linkProfile.removePrefix("/profiles/").removePrefix("/")

    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        val q = query.trim()
        return title.contains(q, ignoreCase = true) ||
            channel.contains(q, ignoreCase = true) ||
            nameProfile.contains(q, ignoreCase = true)
    }

    fun withDuration(newDuration: String): ItemsX = copy(duration = newDuration)
    fun withViews(newViews: String): ItemsX = copy(views = newViews)
    fun withHref(newHref: String): ItemsX = copy(href = newHref)

    fun isSameVideo(other: ItemsX?): Boolean = other != null && id > 0L && id == other.id


    companion object {
        val EMPTY = ItemsX()
    }
}
