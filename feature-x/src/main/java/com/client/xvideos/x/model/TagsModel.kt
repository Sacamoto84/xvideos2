package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable

/**
 * Элемент автора, канала или порнозвезды в блоке тегов под видеороликом.
 *
 * @property href Относительная ссылка на профиль автора или страницу модели.
 * @property name Отображаемое имя автора/модели.
 * @property count Число подписчиков или количество роликов (текстовое представление, например `"359k"`).
 */
@Immutable
data class TagsMainUploaderPornstar(
    val href: String = "",
    val name: String = "",
    val count: String = ""
) {
    val isValid: Boolean get() = href.isNotBlank() && name.isNotBlank()
    val hasHref: Boolean get() = href.isNotBlank()
    val hasName: Boolean get() = name.isNotBlank()
    val hasCount: Boolean get() = count.isNotBlank()

    companion object {
        val EMPTY = TagsMainUploaderPornstar()
    }
}

/**
 * Совокупность всех тегов и метаданных участников, извлеченных со страницы видеоролика.
 *
 * @property mainUploader Список основных авторов/каналов, загрузивших видео.
 * @property pornstars Список порнозвезд и моделей, участвующих в ролике.
 * @property tags Список текстовых категорий и тегов ролика.
 */
@Immutable
data class TagsModel(
    val mainUploader: List<TagsMainUploaderPornstar> = emptyList(),
    val pornstars: List<TagsMainUploaderPornstar> = emptyList(),
    val tags: List<String> = emptyList()
) {
    val isEmpty: Boolean get() = mainUploader.isEmpty() && pornstars.isEmpty() && tags.isEmpty()
    val isNotEmpty: Boolean get() = !isEmpty
    val hasMainUploader: Boolean get() = mainUploader.isNotEmpty()
    val hasPornstars: Boolean get() = pornstars.isNotEmpty()
    val hasTags: Boolean get() = tags.isNotEmpty()
    val hasMultipleTags: Boolean get() = tags.size > 1
    val totalCount: Int get() = mainUploader.size + pornstars.size + tags.size

    companion object {
        val EMPTY = TagsModel()
    }
}
