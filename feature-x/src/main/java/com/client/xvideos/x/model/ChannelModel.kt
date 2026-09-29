package com.client.xvideos.x.model

import androidx.compose.runtime.Immutable

import java.io.Serializable

/**
 * Тип профиля автора в разделе X: студийный канал или порнозвезда/модель.
 */
enum class ProfileType : Serializable {
    CHANNEL,
    MODEL,
}

/**
 * Элемент модели/актрисы или канала-партнёра для фильтрации видео в профиле.
 *
 * @property idUser Уникальный идентификатор пользователя/модели в системе X.
 * @property displayName Отображаемое имя модели или канала.
 * @property gender Пол («Woman», «Man» или пустая строка для каналов).
 * @property nbVideos Количество видео с участием этой модели/канала.
 * @property fNbVideos Форматированное количество видео (строка, например «413»).
 */
@kotlinx.serialization.Serializable
@Immutable
data class ChannelModelFilterItem(
    val idUser: Long = 0L,
    val displayName: String = "",
    val gender: String = "",
    val nbVideos: Int = 0,
    val fNbVideos: String = "",
) : Serializable {
    val isWoman: Boolean get() = gender.equals("Woman", ignoreCase = true)
    val isMan: Boolean get() = gender.equals("Man", ignoreCase = true)
    val countText: String get() = fNbVideos.ifBlank { nbVideos.takeIf { it > 0 }?.toString().orEmpty() }
    val formattedTitle: String get() = if (countText.isNotBlank()) "$displayName ($countText)" else displayName

    fun matches(query: String): Boolean =
        query.isBlank() || displayName.contains(query.trim(), ignoreCase = true)
}

/**
 * Ссылка на автора, студию или модель из блока сотрудничества («Работал для/с»).
 *
 * @property name Отображаемое имя партнёра (например, `"Loupan Producoes"`, `"Pernocas"`).
 * @property href Ссылка или относительный путь (например, `"/profiles/loupan-1"`, `"/models/joy-sky"`).
 */
@kotlinx.serialization.Serializable
@Immutable
data class ChannelCollaborator(
    val name: String = "",
    val href: String = "",
    val isModel: Boolean = false,
) : Serializable {
    /** Извлекает чистый slug без URL-префиксов и параметров. */
    val cleanSlug: String get() = href
        .removePrefix("/profiles/")
        .removePrefix("profiles/")
        .removePrefix("/channels/")
        .removePrefix("channels/")
        .removePrefix("/models/")
        .removePrefix("models/")
        .removePrefix("/pornstars/")
        .removePrefix("pornstars/")
        .removePrefix("/")
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .trim()
}

/**
 * Метаданные шапки профиля канала или актрисы/модели X.
 *
 * @property slug Идентификатор канала/модели в URL (например, `"dart_oficial"` или `"joy-sky"`).
 * @property name Отображаемое имя канала/модели (например, `"Dartoficial"`, `"Joy Ski"`).
 * @property bannerUrl Ссылка на широкоформатное фоновое изображение/баннер (может быть пустым).
 * @property avatarUrl Ссылка на аватар профиля.
 * @property subscribers Текстовое представление количества подписчиков (например, `"10,1 к"`).
 * @property totalViews Текстовое представление суммарных просмотров роликов (например, `"22 М"`).
 * @property aboutMe Текст блока «Обо мне».
 * @property videoCount Общее количество доступных бесплатных видео.
 * @property profileType Тип профиля ([ProfileType.CHANNEL] или [ProfileType.MODEL]).
 * @property gender Пол модели (например, `"Женщина"`).
 * @property age Возраст модели (например, `"26 лет"`).
 * @property country Страна модели (например, `"Бразилия"`).
 * @property workedWith Список партнеров/студий, с кем работала модель.
 * @property collaborators Структурированный список партнеров/студий с ссылками.
 * @property availableModels Список моделей или каналов, доступных для фильтрации видео.
 * @property rankings Список категорий рейтингов модели или канала.
 */
@Immutable
data class ChannelHeaderModel(
    val slug: String = "",
    val name: String = "",
    val bannerUrl: String = "",
    val avatarUrl: String = "",
    val subscribers: String = "",
    val totalViews: String = "",
    val aboutMe: String = "",
    val videoCount: Int = 0,
    val profileType: ProfileType = ProfileType.CHANNEL,
    val gender: String = "",
    val age: String = "",
    val country: String = "",
    val countryCode: String = "",
    val workedWith: String = "",
    val collaborators: List<ChannelCollaborator> = emptyList(),
    val availableModels: List<ChannelModelFilterItem> = emptyList(),
    val rankings: List<ChannelRankingCategory> = emptyList(),
) : Serializable {
    val isModel: Boolean get() = profileType == ProfileType.MODEL
    val hasBanner: Boolean get() = bannerUrl.isNotBlank()
    val hasAvatar: Boolean get() = avatarUrl.isNotBlank()
    val hasAboutMe: Boolean get() = aboutMe.isNotBlank()
    val hasSubscribers: Boolean get() = subscribers.isNotBlank()
    val hasTotalViews: Boolean get() = totalViews.isNotBlank()
    val hasGender: Boolean get() = gender.isNotBlank()
    val hasAge: Boolean get() = age.isNotBlank()
    val hasCountry: Boolean get() = country.isNotBlank()
    val hasWorkedWith: Boolean get() = workedWith.isNotBlank() || collaborators.isNotEmpty()
    val hasCollaborators: Boolean get() = collaborators.isNotEmpty()
    val hasAvailableModels: Boolean get() = availableModels.isNotEmpty()
    val hasRankings: Boolean get() = rankings.isNotEmpty()
    val displayName: String get() = name.ifBlank { slug }
    val flagEmoji: String get() = getFlagEmojiOrNull(countryCode).orEmpty()
    val hasFlag: Boolean get() = flagEmoji.isNotBlank()

    /** Формирует подзаголовок модели, например: "Женщина, Бразилия, 26 лет". */
    val subtitle: String get() {
        val parts = listOfNotNull(
            gender.takeIf { it.isNotBlank() },
            country.takeIf { it.isNotBlank() },
            age.takeIf { it.isNotBlank() },
        )
        return parts.joinToString(", ")
    }

    companion object {
        val EMPTY = ChannelHeaderModel()
    }
}

/**
 * Элемент рейтинга модели или канала (например: Мировой #342).
 *
 * @property rank Числовой номер в рейтинге (например, 342).
 * @property geo Название региона/страны (например, «Мировой», «Латинский», «Бразилия»).
 * @property link Относительная ссылка на страницу каталога/индекса рейтинга (например, `"/porn-actresses-index/from/brazil/ever"`).
 * @property label Описание/подсказка (например, `"Топ 76 230 порноактрис"`).
 */
@kotlinx.serialization.Serializable
@Immutable
data class ChannelRankItem(
    val rank: Int = 0,
    val geo: String = "",
    val link: String = "",
    val label: String = "",
) : Serializable {
    val formattedRank: String get() = "# $rank"
    val cleanLink: String get() = link.trim()
}

/**
 * Подгруппа рейтингов по региону (например: «Только из Бразилия», «По всему миру»).
 *
 * @property label Название подгруппы (например, `"Только из Бразилия"`).
 * @property ranks Список элементов рейтинга.
 */
@kotlinx.serialization.Serializable
@Immutable
data class ChannelRankGroup(
    val label: String = "",
    val ranks: List<ChannelRankItem> = emptyList(),
) : Serializable

/**
 * Категория рейтингов (например: «Рейтинги порноактрис», «Глобальные рейтинги»).
 *
 * @property label Название категории.
 * @property ranks Список подгрупп рейтингов.
 */
@kotlinx.serialization.Serializable
@Immutable
data class ChannelRankingCategory(
    val label: String = "",
    val ranks: List<ChannelRankGroup> = emptyList(),
) : Serializable

@kotlinx.serialization.Serializable
internal data class ChannelRanksResponseDto(
    val result: Boolean = false,
    val code: Int = 0,
    val rankings: List<ChannelRankingCategory> = emptyList(),
)

/**
 * Режим сортировки видеороликов канала.
 *
 * @property apiKey Ключ для запроса к JSON API сайта (`/channels/{slug}/videos/{sort}/{page}`).
 * @property title Отображаемое название вкладки в интерфейсе.
 */
enum class ChannelSortOrder(val apiKey: String, val title: String) : Serializable {
    BEST("best", "Свежие"),
    NEW("new", "Новые"),
    RATING("rating", "Топ"),
}

/**
 * Полное состояние пользовательского интерфейса экрана канала.
 *
 * @property header Метаданные шапки канала.
 * @property videos Загруженный список видеороликов.
 * @property currentSort Текущий выбранный режим сортировки.
 * @property selectedModel Выбранная модель для фильтрации видео (null — показ всех видео).
 * @property modelFilterQuery Поисковый запрос в поле выбора модели.
 * @property isModelFilterExpanded Открыт ли выпадающий список выбора модели.
 * @property isLoadingInitial Флаг начальной загрузки (шапка + первая порция видео).
 * @property isLoadingMore Флаг догрузки следующей страницы при бесконечном скролле.
 * @property isEndReached Флаг окончания списка (следующих страниц больше нет).
 * @property error Текст ошибки в случае сетевого сбоя (null, если ошибки нет).
 */
@Immutable
data class ChannelUiState(
    val header: ChannelHeaderModel = ChannelHeaderModel.EMPTY,
    val videos: List<ItemsX> = emptyList(),
    val currentSort: ChannelSortOrder = ChannelSortOrder.BEST,
    val selectedModel: ChannelModelFilterItem? = null,
    val modelFilterQuery: String = "",
    val isModelFilterExpanded: Boolean = false,
    val currentPage: Int = 0,
    val totalVideosCount: Int = 0,
    val isLoadingInitial: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isEndReached: Boolean = false,
    val error: String? = null,
) : Serializable {
    val availableModels: List<ChannelModelFilterItem> get() = header.availableModels
    val hasModelFilters: Boolean get() = availableModels.isNotEmpty()

    val filteredModels: List<ChannelModelFilterItem> get() = if (modelFilterQuery.isBlank()) {
        availableModels
    } else {
        availableModels.filter { it.matches(modelFilterQuery) }
    }

    val maxPages: Int
        get() {
            val total = when {
                totalVideosCount > 0 -> totalVideosCount
                selectedModel != null && selectedModel.nbVideos > 0 -> selectedModel.nbVideos
                header.videoCount > 0 -> header.videoCount
                else -> 0
            }
            val perPage = 36
            return when {
                total > 0 -> ((total + perPage - 1) / perPage).coerceAtLeast(1)
                videos.size >= 20 -> maxOf(currentPage + 2, 10)
                else -> maxOf(currentPage + 1, 1)
            }
        }

    val isEmpty: Boolean get() = !isLoadingInitial && videos.isEmpty() && error == null
    val isSuccess: Boolean get() = !isLoadingInitial && error == null
}
