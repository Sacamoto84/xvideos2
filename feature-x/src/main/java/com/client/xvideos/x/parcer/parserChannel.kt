package com.client.xvideos.x.parcer

import com.client.xvideos.x.model.ChannelCollaborator
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.urlStart
import kotlinx.serialization.builtins.ListSerializer
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser

/**
 * Извлекает метаданные профиля канала (баннер, аватарку, имя, подписчиков, просмотры, описание, список моделей) из HTML разметки.
 *
 * @param html Текст HTML-страницы канала (например, `/channels/dart_oficial`).
 * @param fallbackSlug Идентификатор канала по умолчанию.
 * @param fallbackName Отображаемое имя автора по умолчанию (из чипа в плеере).
 * @param fallbackSubscribers Число подписчиков по умолчанию (из чипа в плеере).
 * @return [ChannelHeaderModel] с заполненными полями профиля канала.
 */
fun parserChannelHeader(
    html: String,
    fallbackSlug: String = "",
    fallbackName: String = "",
    fallbackSubscribers: String = "",
    isModel: Boolean = false,
): ChannelHeaderModel {
    if (html.isBlank()) {
        return ChannelHeaderModel(
            slug = fallbackSlug,
            name = fallbackName.ifBlank { fallbackSlug },
            subscribers = fallbackSubscribers,
            profileType = if (isModel) com.client.xvideos.x.model.ProfileType.MODEL else com.client.xvideos.x.model.ProfileType.CHANNEL,
        )
    }

    val header = parserChannelHeader(
        document = Jsoup.parse(html),
        fallbackSlug = fallbackSlug,
        fallbackName = fallbackName,
        fallbackSubscribers = fallbackSubscribers,
        isModel = isModel,
    )

    val models = parseChannelModels(html)
    return if (models.isNotEmpty()) {
        val modelNames = models.map { it.displayName.lowercase().trim() }.filter { it.isNotBlank() }.toSet()
        val updatedCollaborators = header.collaborators.map { c ->
            if (!c.isModel && modelNames.contains(c.name.lowercase().trim())) {
                c.copy(isModel = true)
            } else {
                c
            }
        }
        header.copy(availableModels = models, collaborators = updatedCollaborators)
    } else {
        header
    }
}

/**
 * Разбирает DOM документ страницы канала или модели для построения [ChannelHeaderModel].
 */
fun parserChannelHeader(
    document: Document,
    fallbackSlug: String = "",
    fallbackName: String = "",
    fallbackSubscribers: String = "",
    isModel: Boolean = false,
): ChannelHeaderModel {
    // 1. Баннер-обложка (cover banner)
    val bannerImg = document.selectFirst("#profile-title .banner-slider img")
        ?: document.selectFirst(".banner-slider img")
        ?: document.selectFirst("a.banner-slide img")
        ?: document.selectFirst("picture.img-responsive img")
    var bannerUrl = bannerImg?.attr("src")?.trim().orEmpty()
    if (bannerUrl.startsWith("//")) bannerUrl = "https:$bannerUrl"

    // 2. Аватарка (profile picture)
    val avatarImg = document.selectFirst(".profile-infos .profile-pic img")
        ?: document.selectFirst(".profile-pic img")
    var avatarUrl = avatarImg?.attr("src")?.trim().orEmpty()
    if (avatarUrl.startsWith("//")) avatarUrl = "https:$avatarUrl"

    // 3. Отображаемое имя канала / модели
    val nameText = document.selectFirst(".profile-infos h2 strong.text-danger")?.text()?.trim()
        ?: document.selectFirst("h2.with-aka strong")?.text()?.trim()
        ?: document.selectFirst(".profile-infos h2 strong")?.text()?.trim()
        ?: document.selectFirst("h2 strong")?.text()?.trim()
    val resolvedName = nameText?.takeIf { it.isNotBlank() }
        ?: fallbackName.takeIf { it.isNotBlank() }
        ?: fallbackSlug

    // 4. Число подписчиков
    val subsText = document.selectFirst(".profile-infos .user-subscribe .count")?.text()?.trim()
        ?: document.selectFirst("#pinfo-subscribers span")?.text()?.trim()
    val resolvedSubscribers = subsText?.takeIf { it.isNotBlank() }
        ?: fallbackSubscribers

    // 5. Суммарные просмотры
    val totalViews = document.selectFirst("#pinfo-videos-views span")?.text()?.trim()
        ?: document.selectFirst(".profile-infos small span.mobile-hide")?.text()?.trim()
        ?: ""

    // 6. Блок «Обо мне» (текстовое описание)
    val aboutEl = document.selectFirst("#header-about-me")
        ?: document.selectFirst("#pinfo-aboutme")
    val aboutMeText = if (aboutEl != null) {
        val clone = aboutEl.clone()
        clone.select(".show-more").remove()
        clone.select("br").append("\\n")
        val raw = clone.text().replace("\\n", "\n").trim()
        Parser.unescapeEntities(raw, false)
    } else {
        ""
    }

    // 7. Количество видео
    val videoCount = document.selectFirst("#tab-videos .count")?.text()?.trim()?.toIntOrNull() ?: 0

    // 8. Специфичные данные модели/актрисы
    val isModelDetected = isModel ||
        document.html().contains("\"model\":true") ||
        document.selectFirst("a[href*='/models/']") != null ||
        document.selectFirst("h2.is-pornstar") != null ||
        document.selectFirst(".is-pornstar") != null ||
        document.selectFirst("#pinfo-sex") != null ||
        fallbackSlug.startsWith("model")
    val profileType = if (isModelDetected) com.client.xvideos.x.model.ProfileType.MODEL else com.client.xvideos.x.model.ProfileType.CHANNEL

    val gender = document.selectFirst("#pinfo-sex span")?.text()?.trim().orEmpty()
    val age = document.selectFirst("#pinfo-age span")?.text()?.trim().orEmpty()
    val country = document.selectFirst("#pinfo-country span")?.text()?.trim().orEmpty()
    val countryCode = parseProfileCountryCode(document, country)
    val workedWith = document.selectFirst("#pinfo-workedfor span")?.text()?.trim().orEmpty()
    val isCurrentPageChannel = profileType == com.client.xvideos.x.model.ProfileType.CHANNEL
    val collaboratorElements = document.select("#pinfo-workedfor span a[href]")
    val collaborators = collaboratorElements.mapNotNull { a ->
        val href = a.attr("href").trim()
        val name = a.text().trim()
        if (name.isNotBlank()) {
            val isCollaboratorModel = detectCollaboratorIsModel(
                name = name,
                href = href,
                isCurrentPageChannel = isCurrentPageChannel,
            )
            ChannelCollaborator(name = name, href = href, isModel = isCollaboratorModel)
        } else null
    }

    return ChannelHeaderModel(
        slug = fallbackSlug,
        name = resolvedName,
        bannerUrl = bannerUrl,
        avatarUrl = avatarUrl,
        subscribers = resolvedSubscribers,
        totalViews = totalViews,
        aboutMe = aboutMeText,
        videoCount = videoCount,
        profileType = profileType,
        gender = gender,
        age = age,
        country = country,
        countryCode = countryCode,
        workedWith = workedWith,
        collaborators = collaborators,
    )
}

private val STUDIO_KEYWORDS = setOf(
    "studio", "studios", "productions", "production", "producoes", "producao",
    "films", "film", "media", "tv", "canal", "channel", "channels", "network",
    "porn", "xxx", "amador", "amateurs", "entertainment", "magazine", "pictures",
    "club", "clube", "cuckold"
)

internal fun detectCollaboratorIsModel(
    name: String,
    href: String,
    isCurrentPageChannel: Boolean,
    knownModelNames: Set<String> = emptySet(),
): Boolean {
    val lowerHref = href.lowercase().trim()
    val lowerName = name.lowercase().trim()

    // 1. Явные указатели на модель в URL или slug
    if (lowerHref.contains("/models/") || lowerHref.contains("/pornstars/")) return true
    if (lowerHref.endsWith("-model") || lowerHref.endsWith("_model") ||
        lowerHref.contains("-model/") || lowerHref.contains("_model/") ||
        lowerHref.contains("/model-") || lowerHref.contains("/model_")
    ) return true

    // 2. Явные указатели на канал в URL
    if (lowerHref.contains("/channels/")) return false

    // 3. Совпадение с зарегистрированными моделями канала
    if (knownModelNames.isNotEmpty()) {
        val cleanSlug = lowerHref.substringAfterLast('/').substringBefore('?').substringBefore('#').trim()
        if (lowerName in knownModelNames || cleanSlug in knownModelNames) {
            return true
        }
    }

    // 4. Студийные маркеры в названии или slug
    val nameTokens = lowerName.split(' ', '_', '-', '.', '/')
    val slugTokens = lowerHref.split('/', '_', '-', '.')
    val hasStudioMarker = (nameTokens + slugTokens).any { it in STUDIO_KEYWORDS } ||
        STUDIO_KEYWORDS.any { lowerName.contains(it) || lowerHref.contains(it) }

    if (hasStudioMarker) return false

    // 5. Контекст страницы: на странице канала список «Работал для/с» перечисляет моделей студии
    if (isCurrentPageChannel) {
        return true
    }

    // На странице модели, если нет маркеров студии, участник считается коллегой-моделью
    return true
}

private val FLAG_CLASS_REGEX = Regex("""\bflag-([a-z]{2})\b""", RegexOption.IGNORE_CASE)

private val KNOWN_COUNTRY_NAMES_TO_CODE = mapOf(
    "россия" to "ru", "russia" to "ru",
    "бразилия" to "br", "brazil" to "br",
    "сша" to "us", "usa" to "us", "соединенные штаты" to "us", "united states" to "us",
    "украина" to "ua", "ukraine" to "ua",
    "франция" to "fr", "france" to "fr",
    "германия" to "de", "germany" to "de",
    "италия" to "it", "italy" to "it",
    "испания" to "es", "spain" to "es",
    "великобритания" to "gb", "united kingdom" to "gb", "uk" to "gb",
    "канада" to "ca", "canada" to "ca",
    "япония" to "jp", "japan" to "jp",
    "чехия" to "cz", "чешская республика" to "cz", "czech republic" to "cz", "czechia" to "cz",
    "мексика" to "mx", "mexico" to "mx",
    "аргентина" to "ar", "argentina" to "ar",
    "колумбия" to "co", "colombia" to "co",
    "австралия" to "au", "australia" to "au",
    "нидерланды" to "nl", "netherlands" to "nl",
    "бельгия" to "be", "belgium" to "be",
    "польша" to "pl", "poland" to "pl",
    "швеция" to "se", "sweden" to "se",
    "швейцария" to "ch", "switzerland" to "ch",
    "австрия" to "at", "austria" to "at",
    "венгрия" to "hu", "hungary" to "hu",
    "португалия" to "pt", "portugal" to "pt",
    "китай" to "cn", "china" to "cn",
    "южная корея" to "kr", "корея" to "kr", "south korea" to "kr",
    "индия" to "in", "india" to "in",
    "румыния" to "ro", "romania" to "ro",
    "сербия" to "rs", "serbia" to "rs",
    "словакия" to "sk", "slovakia" to "sk",
    "таиланд" to "th", "thailand" to "th",
    "тайвань" to "tw", "taiwan" to "tw",
    "турция" to "tr", "turkey" to "tr",
    "чили" to "cl", "chile" to "cl",
    "эквадор" to "ec", "ecuador" to "ec",
    "венесуэла" to "ve", "venezuela" to "ve",
    "беларусь" to "by", "belarus" to "by",
    "казахстан" to "kz", "kazakhstan" to "kz",
    "израиль" to "il", "israel" to "il",
    "южная африка" to "za", "south africa" to "za",
    "филиппины" to "ph", "philippines" to "ph",
    "вьетнам" to "vn", "vietnam" to "vn",
    "греция" to "gr", "greece" to "gr",
    "норвегия" to "no", "norway" to "no",
    "дания" to "dk", "denmark" to "dk",
    "финляндия" to "fi", "finland" to "fi",
    "ирландия" to "ie", "ireland" to "ie",
)

internal fun parseProfileCountryCode(document: Document, countryName: String): String {
    val flagEl = document.selectFirst(".profile-infos h2 span[class*='flag']")
        ?: document.selectFirst("h2.with-aka span[class*='flag']")
        ?: document.selectFirst("h2 span[class*='flag']")
        ?: document.selectFirst("#pinfo-country span[class*='flag']")
        ?: document.selectFirst(".profile-infos span.flag")
        ?: document.selectFirst("#pinfo-country span.flag")

    val classString = flagEl?.className().orEmpty()
    val match = FLAG_CLASS_REGEX.find(classString)
    if (match != null) {
        val code = match.groupValues[1].lowercase()
        if (com.client.xvideos.x.model.isIsoCountryCode(code)) return code
    }

    val trimmedCountry = countryName.trim().lowercase()
    if (trimmedCountry.isNotBlank()) {
        val mapped = KNOWN_COUNTRY_NAMES_TO_CODE[trimmedCountry]
        if (mapped != null) return mapped
        if (com.client.xvideos.x.model.isIsoCountryCode(trimmedCountry)) return trimmedCountry
    }
    return ""
}

private val channelJson = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

@kotlinx.serialization.Serializable
internal data class ChannelVideosResponseDto(
    val videos: List<ChannelVideoItemDto> = emptyList(),
    val nb_videos: Int? = null,
    val current_page: Int? = null,
)

@kotlinx.serialization.Serializable
internal data class ChannelVideoItemDto(
    val id: Long = 0L,
    val u: String = "",
    val tf: String = "",
    val t: String = "",
    val i: String = "",
    val ipu: String = "",
    val d: String = "",
    val n: String = "",
    val r: String = "",
    val pn: String = "",
    val pu: String = "",
)

/**
 * Результат парсинга ответа видеоленты канала/модели.
 *
 * @property videos Список видеороликов.
 * @property totalVideos Общее количество видео автора (если возвращено сервером).
 * @property currentPage Номер текущей страницы от сервера.
 */
data class ChannelVideosResult(
    val videos: List<ItemsX> = emptyList(),
    val totalVideos: Int? = null,
    val currentPage: Int? = null,
)

/**
 * Разбирает ответ JSON API канала (`/channels/{slug}/videos/{sort}/{page}`) в структуру [ChannelVideosResult].
 *
 * @param jsonString Текст ответа в формате JSON.
 * @return [ChannelVideosResult] со списком роликов и метаинформацией пагинации.
 */
fun parserChannelVideosResult(jsonString: String): ChannelVideosResult {
    if (jsonString.isBlank()) return ChannelVideosResult()
    val trimmed = jsonString.trim()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return ChannelVideosResult()

    return try {
        val dto = channelJson.decodeFromString(ChannelVideosResponseDto.serializer(), trimmed)
        val list = dto.videos
        val result = ArrayList<ItemsX>(list.size)

        for (item in list) {
            val rawTitle = item.tf.ifBlank { item.t }.trim()
            val cleanTitle = Parser.unescapeEntities(rawTitle, false)
            val rawUrl = item.u.trim()
            val fullHref = when {
                rawUrl.startsWith("http") -> normalizeXUrl(rawUrl)
                rawUrl.isNotBlank() -> normalizeXUrl("$urlStart$rawUrl")
                else -> ""
            }

            result.add(
                ItemsX(
                    id = item.id,
                    title = cleanTitle,
                    href = fullHref,
                    previewImage = item.i.trim(),
                    previewVideo = item.ipu.trim(),
                    duration = item.d.trim(),
                    views = item.n.trim(),
                    channel = item.pn.trim(),
                    nameProfile = item.pn.trim(),
                    linkProfile = item.pu.trim(),
                )
            )
        }
        ChannelVideosResult(
            videos = result,
            totalVideos = dto.nb_videos,
            currentPage = dto.current_page,
        )
    } catch (_: Exception) {
        ChannelVideosResult()
    }
}

/**
 * Разбирает ответ JSON API канала (`/channels/{slug}/videos/{sort}/{page}`) в список моделей [ItemsX].
 *
 * @param jsonString Текст ответа в формате JSON.
 * @return Список видеороликов [ItemsX].
 */
fun parserChannelVideosJson(jsonString: String): List<ItemsX> =
    parserChannelVideosResult(jsonString).videos

/**
 * Извлекает список доступных для фильтрации моделей/партнёров из JSON-конфигурации страницы канала.
 *
 * @param html Текст HTML-страницы канала.
 * @return Список элементов [ChannelModelFilterItem].
 */
fun parseChannelModels(html: String): List<ChannelModelFilterItem> {
    if (html.isBlank()) return emptyList()
    val keyIndex = html.indexOf("\"workedForFree\"").takeIf { it >= 0 }
        ?: html.indexOf("'workedForFree'").takeIf { it >= 0 }
        ?: return emptyList()
    val arrayStart = html.indexOf('[', keyIndex).takeIf { it >= 0 } ?: return emptyList()
    var depth = 0
    var arrayEnd = -1
    for (i in arrayStart until html.length) {
        when (html[i]) {
            '[' -> depth++
            ']' -> {
                depth--
                if (depth == 0) {
                    arrayEnd = i + 1
                    break
                }
            }
        }
    }
    if (arrayEnd <= arrayStart) return emptyList()
    val rawJson = html.substring(arrayStart, arrayEnd)
    return try {
        channelJson.decodeFromString(
            ListSerializer(ChannelModelFilterItem.serializer()),
            rawJson
        )
    } catch (_: Exception) {
        emptyList()
    }
}

/**
 * Разбирает ответ JSON API рейтингов канала или модели (`/profiles/{username}/ranks/straight`)
 * в структурированный список категорий [com.client.xvideos.x.model.ChannelRankingCategory].
 *
 * @param jsonString Текст ответа в формате JSON.
 * @return Список категорий рейтингов.
 */
fun parserChannelRanksJson(jsonString: String): List<com.client.xvideos.x.model.ChannelRankingCategory> {
    if (jsonString.isBlank()) return emptyList()
    val trimmed = jsonString.trim()
    if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return emptyList()

    return try {
        val dto = channelJson.decodeFromString(com.client.xvideos.x.model.ChannelRanksResponseDto.serializer(), trimmed)
        if (dto.result) dto.rankings else emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}


