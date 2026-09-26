package com.client.xvideos.r.model.search

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ответ сетевого эндпоинта поиска/подсказок авторов (`/v2/creators/suggest?query=...`).
 *
 * @property items Список найденных авторов [SearchItemCreatorsResponse].
 */
@Immutable
@Serializable
data class SearchCreatorsResponse(
    @SerialName("items") val items: List<SearchItemCreatorsResponse> = emptyList()
) {
    /** Проверяет пустоту списка авторов. */
    val isEmpty: Boolean get() = items.isEmpty()

    /** Проверяет непустоту списка авторов. */
    val isNotEmpty: Boolean get() = items.isNotEmpty()

    /** Количество авторов в выдаче. */
    val size: Int get() = items.size

    /** Количество авторов в выдаче. */
    val count: Int get() = items.size

    /** Проверяет наличие хотя бы одного автора в выдаче. */
    val hasItems: Boolean get() = items.isNotEmpty()

    /** Первый автор в выдаче или null. */
    val firstOrNull: SearchItemCreatorsResponse? get() = items.firstOrNull()

    /**
     * Поиск автора по никнейму (с префиксом '@' или без него) без учета регистра.
     *
     * @param username Никнейм автора.
     * @return Найденный элемент [SearchItemCreatorsResponse] или null.
     */
    fun findByUsernameOrNull(username: String?): SearchItemCreatorsResponse? {
        if (username.isNullOrBlank()) return null
        val clean = username.removePrefix("@").trim()
        return items.firstOrNull { it.username.equals(clean, ignoreCase = true) }
    }

    companion object {
        /** Пустой экземпляр ответа. */
        val EMPTY = SearchCreatorsResponse()
    }
}

//{
//    "type": "creator",
//    "text": "@elfsandi",
//    "name": "Ana \ud83d\udc8b",
//    "image": "https:\/\/userpic.redgifs.com\/5\/3f\/53f9367f4b1d523a032f5fa2475de70d.png",
//    "verified": true,
//    "studio": false,
//    "followers": 274
//},
//{
//    "type": "creator",
//    "text": "@ana-fernandez",
//    "name": "ana-fernandez",
//    "image": null,
//    "verified": false,
//    "studio": false,
//    "followers": 77
//},
/**
 * Элемент подсказки/поиска автора в поисковой строке.
 *
 * @property type Тип элемента (обычно "creator").
 * @property text Текст с префиксом "@", например "@elfsandi".
 * @property name Отображаемое имя автора.
 * @property image URL аватара.
 * @property verified Статус верификации автора.
 * @property studio Флаг профессиональной студии.
 * @property followers Число подписчиков.
 */
@Immutable
@Serializable
data class SearchItemCreatorsResponse(
    @SerialName("type") val type: String = "creator",
    @SerialName("text") val text: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("image") val image: String? = null,
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("studio") val studio: Boolean = false,
    @SerialName("followers") val followers: Long = 0L
) {
    /** Никнейм автора без префикса "@". */
    val username: String get() = text.removePrefix("@")

    /** Отображаемое имя либо очищенный никнейм. */
    val displayName: String get() = name.ifBlank { username }

    /** Проверяет валидность элемента (поле text не пусто). */
    val isValid: Boolean get() = text.isNotBlank()

    /** Проверяет наличие непустого текстового поля. */
    val hasText: Boolean get() = text.isNotBlank()

    /** Проверяет наличие отображаемого имени. */
    val hasName: Boolean get() = name.isNotBlank()

    /** Проверяет наличие аватара. */
    val hasImage: Boolean get() = !image.isNullOrBlank()

    /** Проверяет наличие подписчиков. */
    val hasFollowers: Boolean get() = followers > 0L

    companion object {
        /** Пустой экземпляр подсказки создателя. */
        val EMPTY = SearchItemCreatorsResponse()
    }
}
