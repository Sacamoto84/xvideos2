package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Значения по умолчанию обязаны быть **у всех** полей, и это не косметика.
 *
 * Избранное хранится файлами и читается через kotlinx.serialization.
 */
@Immutable
@Serializable
data class ItemsX(
    val id : Long = 0L,               //   Номер 234234233 берется из сайта
    val title : String = "",          // - Название видео(Зависит от выбранного языка)
    val duration : String = "",       // * Длинна видео (11 мин.)
    val views : String = "",          // - Количество просмотров
    val channel : String = "",        // * Отображаемое название канала (Old4k)
    val previewImage : String = "",   // * Путь до картинки превью
    val previewVideo : String = "",   // * Путь до видео превью
    val href: String = "",            // * Путь до страницы видео (Для открытия в экране плеера) Только оно и нужно для этого
    val nameProfile: String = "",     // - Отображаемое название профиля (TODO)
    val linkProfile: String = "",     // * Путь до профиля путь к каналу (/old4k)
)
