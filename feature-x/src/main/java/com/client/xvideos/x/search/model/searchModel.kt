package com.client.xvideos.x.search.model

import kotlinx.serialization.Serializable

/**
 * Подсказка поискового запроса (ключевое слово и его рейтинг популярности).
 *
 * @property N Название ключевого слова / категории.
 * @property R Рейтинг релевантности/популярности запроса.
 */
@Serializable
data class Keyword(val N: String, val R: String) { //N группа R-рейтинг
    val name: String get() = N
    val rating: String get() = R
    val isValid: Boolean get() = N.isNotBlank()
    val hasRating: Boolean get() = R.isNotBlank()
    val ratingDoubleOrNull: Double? get() = R.toDoubleOrNull()
    val normalizedName: String get() = N.trim().lowercase()

    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        return N.contains(query.trim(), ignoreCase = true)
    }

    companion object {
        val EMPTY = Keyword(N = "", R = "")
    }
}

/**
 * Модель найденной порнозвезды / модели в поисковой подсказке.
 *
 * @property N Имя модели.
 * @property F Путь к странице модели (slug URL).
 * @property T Тип записи (`"pornstar"`).
 * @property MV Количество видеороликов модели.
 * @property M Вспомогательный счетчик.
 * @property L Вспомогательный уровень/рейтинг.
 * @property P URL аватара/фотографии модели.
 * @property RF Текстовое количество подписчиков.
 * @property A Дополнительные атрибуты ответа API.
 */
@Serializable
data class Pornstar(
    val N: String,
    val F: String,
    val T: String, //"pornstar"
    val MV: Int,   //Количество видео
    val M: Int,
    val L: Int,
    val P: String, //Путь до картинки
    val RF: String, //Количество подписчиков
    val A: Map<String, String>? = null // Обрабатываем возможное отсутствие поля A
) {
    val name: String get() = N
    val profilePath: String get() = F
    val avatarUrl: String get() = P
    val videoCount: Int get() = MV
    val subscribers: String get() = RF
    val isValid: Boolean get() = N.isNotBlank()
    val hasAvatar: Boolean get() = P.isNotBlank()
    val hasSubscribers: Boolean get() = RF.isNotBlank() && RF != "0"
    val hasVideos: Boolean get() = MV > 0
    val normalizedName: String get() = N.trim().lowercase()
    val cleanProfilePath: String get() = F.removePrefix("/")

    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        return N.contains(query.trim(), ignoreCase = true)
    }

    companion object {
        val EMPTY = Pornstar(N = "", F = "", T = "pornstar", MV = 0, M = 0, L = 0, P = "", RF = "")
    }
}

/**
 * Модель найденного канала/студии в поисковой подсказке.
 *
 * @property N Отображаемое название канала.
 * @property F Относительный путь к странице профиля (`/profiles/xxx`).
 * @property T Тип записи (`"channel"`).
 * @property CPV Флаг премиум/верифицированного канала.
 * @property M Вспомогательный счетчик.
 * @property L Вспомогательный счетчик.
 * @property P URL логотипа/аватара канала.
 * @property RF Текстовое количество подписчиков.
 * @property A Дополнительные атрибуты ответа API.
 */
@Serializable
data class Channel(
    val N: String, //Отображаемое название канала в поисковике
    val F: String, //путь к /profiles/xxx
    val T: String, //Тип "channel"
    val CPV: Boolean, //true
    val M: Int, //0
    val L: Int, //0
    val P: String, // Путь к картинке
    val RF: String, //Количество подписчиков
    val A: Map<String, String>? = null // Дополнительные атрибуты
) {
    val name: String get() = N
    val profilePath: String get() = F
    val avatarUrl: String get() = P
    val subscribers: String get() = RF
    val isValid: Boolean get() = N.isNotBlank()
    val hasAvatar: Boolean get() = P.isNotBlank()
    val hasSubscribers: Boolean get() = RF.isNotBlank() && RF != "0"
    val isCpv: Boolean get() = CPV
    val normalizedName: String get() = N.trim().lowercase()
    val cleanProfilePath: String get() = F.removePrefix("/")

    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        return N.contains(query.trim(), ignoreCase = true)
    }

    companion object {
        val EMPTY = Channel(N = "", F = "", T = "channel", CPV = false, M = 0, L = 0, P = "", RF = "")
    }
}

/**
 * Ответ поискового автодополнения X.
 *
 * @property result Флаг успешности ответа.
 * @property code Числовой код статуса.
 * @property keywords Список предложенных ключевых фраз ([Keyword]).
 * @property pornstar Список предложенных моделей ([Pornstar]).
 * @property channel Список предложенных каналов ([Channel]).
 * @property BLACKLISTED Флаг блокировки запроса в поисковом индексе.
 */
@Serializable
data class SearchResult(
    val result: Boolean = false,
    val code: Int = 0,
    val keywords: List<Keyword> = emptyList(),
    val pornstar: List<Pornstar>? = null, // Может отсутствовать
    val channel: List<Channel>? = null,   // Может отсутствовать
    val BLACKLISTED: Boolean? = null      // Может отсутствовать
) {
    val isEmpty: Boolean get() = keywords.isEmpty() && pornstar.isNullOrEmpty() && channel.isNullOrEmpty()
    val isNotEmpty: Boolean get() = !isEmpty
    val hasKeywords: Boolean get() = keywords.isNotEmpty()
    val hasPornstars: Boolean get() = !pornstar.isNullOrEmpty()
    val hasChannels: Boolean get() = !channel.isNullOrEmpty()
    val isBlacklisted: Boolean get() = BLACKLISTED == true
    val totalSuggestionsCount: Int
        get() = keywords.size + (pornstar?.size ?: 0) + (channel?.size ?: 0)

    fun allSuggestionNames(): List<String> =
        keywords.map { it.name } +
            (pornstar?.map { it.name } ?: emptyList()) +
            (channel?.map { it.name } ?: emptyList())

    fun findPornstarByName(name: String?): Pornstar? {
        if (name.isNullOrBlank()) return null
        return pornstar?.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun findChannelByName(name: String?): Channel? {
        if (name.isNullOrBlank()) return null
        return channel?.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    companion object {
        val EMPTY = SearchResult(result = false, code = 0, keywords = emptyList())
    }
}
