package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.x.xProfileSlug
import kotlinx.serialization.Serializable

/**
 * Элемент сохранённой подписки на канал или актрису/модель X.
 *
 * @property slug Уникальный slug канала/модели (без префиксов).
 * @property name Отображаемое имя автора.
 * @property avatarUrl URL аватара автора.
 * @property bannerUrl URL баннера (если есть).
 * @property isModel Флаг: true — модель/актриса, false — канал/студия.
 * @property subscribers Число подписчиков (например, "12,3 к").
 * @property totalViews Суммарные просмотры (например, "5 М").
 * @property videoCount Количество видео.
 * @property dateAdded Временная метка добавления в подписки.
 */
@Serializable
@Immutable
data class XSubscriptionItem(
    val slug: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val isModel: Boolean = false,
    val subscribers: String = "",
    val totalViews: String = "",
    val videoCount: Int = 0,
    val dateAdded: Long = System.currentTimeMillis(),
) : java.io.Serializable {

    val cleanSlug: String
        get() = xProfileSlug(slug)

    val displayName: String
        get() = name.ifBlank { cleanSlug }
}

/**
 * Преобразует [ChannelHeaderModel] в [XSubscriptionItem] для сохранения в подписки.
 */
fun ChannelHeaderModel.toSubscriptionItem(): XSubscriptionItem = XSubscriptionItem(
    slug = xProfileSlug(slug),
    name = displayName,
    avatarUrl = avatarUrl,
    bannerUrl = bannerUrl,
    isModel = isModel,
    subscribers = subscribers,
    totalViews = totalViews,
    videoCount = videoCount,
)
