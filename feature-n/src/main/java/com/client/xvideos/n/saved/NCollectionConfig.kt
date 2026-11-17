package com.client.xvideos.n.saved

import kotlinx.serialization.Serializable

/**
 * Конфигурация отдельной коллекции страниц манги.
 *
 * @property schemaVersion Версия схемы данных.
 * @property coverPictureId Идентификатор картинки, выбранной пользователем в качестве обложки.
 */
@Serializable
data class NCollectionConfig(
    val schemaVersion: Int = 1,
    val coverPictureId: String? = null
)
