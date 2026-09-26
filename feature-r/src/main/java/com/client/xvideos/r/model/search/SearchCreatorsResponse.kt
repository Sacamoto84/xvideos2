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

    /** Список никнеймов всех авторов в выдаче. */
    val allUsernames: List<String> get() = items.map { it.username }

    /** Список только верифицированных авторов. */
    val verifiedOnly: List<SearchItemCreatorsResponse> get() = items.filter { it.verified }

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

    /**
     * Фильтрует авторов по поисковому запросу.
     */
    fun filterByQuery(query: String?): List<SearchItemCreatorsResponse> =
        if (query.isNullOrBlank()) items else items.filter { it.matches(query) }

    companion object {
        /** Пустой экземпляр ответа. */
        val EMPTY = SearchCreatorsResponse()
    }
}

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

    /** Нормализованный никнейм автора в нижнем регистре без лишних пробелов. */
    val normalizedUsername: String get() = username.trim().lowercase()

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

    /** Проверяет соответствие автора поисковому запросу. */
    fun matches(query: String?): Boolean =
        if (query.isNullOrBlank()) false else displayName.contains(query.trim(), ignoreCase = true) || username.contains(query.trim(), ignoreCase = true)

    /** Форматирует число подписчиков в компактный вид (k, M). */
    fun formatFollowers(): String {
        return when {
            followers >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", followers / 1_000_000.0)
            followers >= 1_000L -> String.format(java.util.Locale.US, "%.1fk", followers / 1_000.0)
            else -> followers.toString()
        }
    }

    /** Преобразует элемент выдачи в модель профиля [com.client.xvideos.r.model.UserInfo]. */
    fun toUserInfo(): com.client.xvideos.r.model.UserInfo =
        com.client.xvideos.r.model.UserInfo(
            username = username,
            name = name,
            profileImageUrl = image,
            verified = verified,
            followers = followers
        )

    companion object {
        /** Пустой экземпляр подсказки создателя. */
        val EMPTY = SearchItemCreatorsResponse()
    }
}
