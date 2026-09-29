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
    val isValidRating: Boolean get() = (ratingDoubleOrNull ?: -1.0) >= 0.0
    val normalizedName: String get() = N.trim().lowercase()

    fun withRating(newRating: String): Keyword = copy(R = newRating)

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
    val hasValidSubscribers: Boolean get() = hasSubscribers
    val hasVideos: Boolean get() = MV > 0
    val normalizedName: String get() = N.trim().lowercase()
    val cleanProfilePath: String get() = F.removePrefix("/")
    val profileUrl: String get() = if (F.startsWith("http")) F else "https://www.xvideos.com/$cleanProfilePath"

    /** Форматирует количество видеороликов модели в компактный вид. */
    fun formatVideos(): String =
        if (videoCount >= 1000) String.format(java.util.Locale.US, "%.1fk", videoCount / 1000.0) else videoCount.toString()

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
    val isVerified: Boolean get() = CPV
    val normalizedName: String get() = N.trim().lowercase()
    val cleanProfilePath: String get() = F.removePrefix("/")
    val profileUrl: String get() = if (F.startsWith("http")) F else "https://www.xvideos.com/$cleanProfilePath"

    fun matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        return N.contains(query.trim(), ignoreCase = true)
    }

    companion object {
        val EMPTY = Channel(N = "", F = "", T = "channel", CPV = false, M = 0, L = 0, P = "", RF = "")
    }
}

/**
 * Контейнер данных подсказок внутри ключа `data` API X.
 */
@Serializable
data class SearchSuggestData(
    val keywords: List<Keyword> = emptyList(),
    val pornstar: List<Pornstar>? = null,
    val channel: List<Channel>? = null,
)

/**
 * Ответ поискового автодополнения X.
 *
 * @property result Флаг успешности ответа.
 * @property code Числовой код статуса.
 * @property data Вложенный объект с результатами (актуальный формат API).
 * @property keywords Список предложенных ключевых фраз ([Keyword]) (плоский формат).
 * @property pornstar Список предложенных моделей ([Pornstar]).
 * @property channel Список предложенных каналов ([Channel]).
 * @property BLACKLISTED Флаг блокировки запроса в поисковом индексе.
 */
@Serializable
data class SearchResult(
    val result: Boolean = false,
    val code: Int = 0,
    val data: SearchSuggestData? = null,
    val keywords: List<Keyword> = emptyList(),
    val pornstar: List<Pornstar>? = null, // Может отсутствовать
    val channel: List<Channel>? = null,   // Может отсутствовать
    val BLACKLISTED: Boolean? = null      // Может отсутствовать
) {
    val resolvedKeywords: List<Keyword>
        get() = if (keywords.isNotEmpty()) keywords else (data?.keywords.orEmpty())

    val resolvedPornstars: List<Pornstar>
        get() = if (!pornstar.isNullOrEmpty()) pornstar else (data?.pornstar.orEmpty())

    val resolvedChannels: List<Channel>
        get() = if (!channel.isNullOrEmpty()) channel else (data?.channel.orEmpty())

    val isEmpty: Boolean get() = resolvedKeywords.isEmpty() && resolvedPornstars.isEmpty() && resolvedChannels.isEmpty()
    val isNotEmpty: Boolean get() = !isEmpty
    val hasKeywords: Boolean get() = resolvedKeywords.isNotEmpty()
    val hasPornstars: Boolean get() = resolvedPornstars.isNotEmpty()
    val hasChannels: Boolean get() = resolvedChannels.isNotEmpty()
    val isBlacklisted: Boolean get() = BLACKLISTED == true
    val totalSuggestionsCount: Int
        get() = resolvedKeywords.size + resolvedPornstars.size + resolvedChannels.size

    val allSuggestions: List<String> get() = allSuggestionNames()

    fun allSuggestionNames(): List<String> =
        resolvedKeywords.map { it.name } +
            resolvedPornstars.map { it.name } +
            resolvedChannels.map { it.name }

    /** Фильтрует ключевые слова, порнозвезд и каналы по поисковому запросу. */
    fun filterByQuery(query: String?): SearchResult {
        if (query.isNullOrBlank()) return this
        val filteredKw = resolvedKeywords.filter { it.matches(query) }
        val filteredPs = resolvedPornstars.filter { it.matches(query) }
        val filteredCh = resolvedChannels.filter { it.matches(query) }
        return copy(
            data = SearchSuggestData(keywords = filteredKw, pornstar = filteredPs, channel = filteredCh),
            keywords = filteredKw,
            pornstar = filteredPs,
            channel = filteredCh
        )
    }

    fun findPornstarByName(name: String?): Pornstar? {
        if (name.isNullOrBlank()) return null
        return resolvedPornstars.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    fun findChannelByName(name: String?): Channel? {
        if (name.isNullOrBlank()) return null
        return resolvedChannels.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }

    companion object {
        val EMPTY = SearchResult(result = false, code = 0, keywords = emptyList())
    }
}
