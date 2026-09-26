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
    val normalizedName: String get() = name.trim().lowercase()
    val cleanHref: String get() = href.removePrefix("/profiles/").removePrefix("/pornstars/").removePrefix("/")

    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        val q = query.trim()
        return name.contains(q, ignoreCase = true)
    }

    fun isSame(other: TagsMainUploaderPornstar?): Boolean =
        other != null && href.isNotBlank() && href == other.href

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
    val tagsCount: Int get() = tags.size
    val mainUploaderCount: Int get() = mainUploader.size
    val pornstarsCount: Int get() = pornstars.size
    val allNames: List<String> get() = mainUploader.map { it.name } + pornstars.map { it.name }

    fun containsTag(tag: String?): Boolean =
        if (tag.isNullOrBlank()) false else tags.any { it.equals(tag, ignoreCase = true) }

    fun filterTags(query: String?): List<String> {
        if (query.isNullOrBlank()) return tags
        val q = query.trim()
        return tags.filter { it.contains(q, ignoreCase = true) }
    }

    fun findPornstarByName(name: String?): TagsMainUploaderPornstar? {
        if (name.isNullOrBlank()) return null
        return pornstars.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }


    companion object {
        val EMPTY = TagsModel()
    }
}
