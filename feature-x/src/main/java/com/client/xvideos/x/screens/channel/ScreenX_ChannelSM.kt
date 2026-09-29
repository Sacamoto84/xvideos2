package com.client.xvideos.x.screens.channel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.x.feature.net.postFormDataFromURLDirect
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.model.ChannelHeaderModel
import com.client.xvideos.x.model.ChannelModelFilterItem
import com.client.xvideos.x.model.ChannelSortOrder
import com.client.xvideos.x.model.ChannelUiState
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.parcer.parserChannelHeader
import com.client.xvideos.x.parcer.parserChannelRanksJson
import com.client.xvideos.x.parcer.parserChannelVideosResult
import com.client.xvideos.x.urlStart
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateSetOf
import com.client.xvideos.x.feature.saved.SavedX
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.toSubscriptionItem
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * ScreenModel экрана канала автора/студии X.
 *
 * Отвечает за загрузку шапки профиля из HTML, параллельную подгрузку видеоленты
 * через официальный JSON API сайта, бесконечную пагинацию, фильтрацию по моделям и смену сортировок.
 *
 * @property saved Фасад локальных данных раздела X для работы с подписками.
 * @property slug Идентификатор канала (например, `"dart_oficial"` или `"/dart_oficial"`).
 * @property initialModel Первичные данные о канале из плеера (имя, число подписчиков).
 */
@Stable
class ScreenX_ChannelSM @AssistedInject constructor(
    val saved: SavedX,
    @Assisted("slug") slug: String,
    @Assisted("initialModel") val initialModel: TagsMainUploaderPornstar? = null,
    @Assisted("isModel") val isModelParam: Boolean = false,
) : ScreenModel {

    /** Флаг, определяющий, является ли профиль страницей порнозвезды/модели. */
    val isModel: Boolean = isModelParam ||
        slug.contains("/models/") ||
        slug.startsWith("models/") ||
        initialModel?.href?.contains("/models/") == true

    /** Нормализованный slug канала/модели без ведущих слешей и префиксов. */
    val cleanSlug: String = slug
        .removePrefix("/models/")
        .removePrefix("models/")
        .removePrefix("/channels/")
        .removePrefix("channels/")
        .removePrefix("/profiles/")
        .removePrefix("profiles/")
        .removePrefix("/")
        .trim()

    val pathPrefix: String get() = if (isModel) "models" else "channels"
    val effectivePathPrefix: String get() = if (uiState.header.isModel) "models" else pathPrefix

    /** Имя POST-параметра фильтрации (для канала фильтруем по модели, для модели — по каналу). */
    private val modelFilterParamKey: String get() = if (uiState.header.isModel || isModel) "idUserChannel" else "idUserModel"

    /** Текущее состояние экрана. */
    var uiState: ChannelUiState by mutableStateOf(
        ChannelUiState(
            header = ChannelHeaderModel(
                slug = cleanSlug,
                name = initialModel?.name?.ifBlank { cleanSlug } ?: cleanSlug,
                subscribers = initialModel?.count.orEmpty(),
                profileType = if (isModel) com.client.xvideos.x.model.ProfileType.MODEL else com.client.xvideos.x.model.ProfileType.CHANNEL,
            ),
            isLoadingInitial = true,
        )
    )
        private set

    /** Проверяет, оформлена ли подписка на текущего автора. */
    val isSubscribed: Boolean
        get() = if (uiState.header.isModel || isModel) {
            saved.subscriptions.containsModel(cleanSlug)
        } else {
            saved.subscriptions.containsChannel(cleanSlug)
        }

    /**
     * Оформляет или отменяет подписку на текущий канал или модель.
     */
    fun toggleSubscription() {
        val item = uiState.header.toSubscriptionItem()
        if (uiState.header.isModel || isModel) {
            if (saved.subscriptions.containsModel(cleanSlug)) {
                saved.subscriptions.removeModel(cleanSlug)
            } else {
                saved.subscriptions.addModel(item.copy(isModel = true))
            }
        } else {
            if (saved.subscriptions.containsChannel(cleanSlug)) {
                saved.subscriptions.removeChannel(cleanSlug)
            } else {
                saved.subscriptions.addChannel(item.copy(isModel = false))
            }
        }
    }

    /**
     * Проверяет, находится ли видеоролик в избранном.
     */
    fun isFavorite(id: Long): Boolean = saved.favorites.contains(id)

    /**
     * Добавляет видеоролик в избранное.
     */
    fun addFavorite(item: ItemsX) {
        saved.favorites.add(item)
    }

    /**
     * Удаляет видеоролик из избранного.
     */
    fun removeFavorite(item: ItemsX) {
        saved.favorites.remove(item)
    }

    /**
     * Скачивает видеоролик в наилучшем качестве в локальное хранилище.
     */
    fun download(item: ItemsX) {
        saved.downloads.download(item)
    }

    /**
     * Сохраняет видеоролик в галерею устройства.
     */
    fun saveToGallery(item: ItemsX) {
        saved.downloads.saveToGallery(item)
    }

    /** Фабрика assisted injection для передачи параметров [slug], [initialModel] и [isModel]. */
    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(
            @Assisted("slug") slug: String,
            @Assisted("initialModel") initialModel: TagsMainUploaderPornstar? = null,
            @Assisted("isModel") isModel: Boolean = false,
        ): ScreenX_ChannelSM
    }

    private var initialJob: Job? = null
    private val pageJobs = mutableMapOf<Int, Job>()
    var currentPage: Int = 0

    /** Кэш загруженных страниц видеороликов: pageIndex -> List<ItemsX>. */
    val pagesCache = mutableStateMapOf<Int, List<ItemsX>>()

    /** Множество номеров страниц, находящихся в процессе сетевой загрузки. */
    val loadingPages = mutableStateSetOf<Int>()

    /** Карта ошибок загрузки страниц: pageIndex -> текст ошибки. */
    val errorPages = mutableStateMapOf<Int, String>()

    /** Сохранённые состояния скролла для каждой страницы. */
    val gridStates = mutableMapOf<Int, LazyGridState>()

    fun getGridState(page: Int): LazyGridState = gridStates.getOrPut(page) { LazyGridState() }

    init {
        loadInitial()
    }

    /**
     * Загружает JSON порцию видеороликов через GET (без фильтра) либо POST (с фильтром по модели).
     */
    private suspend fun fetchVideosJson(page: Int, overridePrefix: String? = null): String {
        val prefix = overridePrefix ?: effectivePathPrefix
        val sortKey = uiState.currentSort.apiKey
        val targetUrl = "$urlStart/$prefix/$cleanSlug/videos/$sortKey/$page"
        val selected = uiState.selectedModel
        return withContext(Dispatchers.IO) {
            if (selected != null && selected.idUser > 0L) {
                postFormDataFromURLDirect(
                    url = targetUrl,
                    formParameters = mapOf(modelFilterParamKey to selected.idUser.toString())
                )
            } else {
                readHtmlFromURLDirect(targetUrl)
            }
        }
    }

    /**
     * Загружает HTML-разметку страницы канала или модели с fallback-проверками альтернативных путей.
     */
    private suspend fun fetchChannelHtml(cleanSlug: String, pathPrefix: String, isModel: Boolean): String = withContext(Dispatchers.IO) {
        val raw = readHtmlFromURLDirect("$urlStart/$pathPrefix/$cleanSlug")
        if (raw.isNotBlank() && !raw.contains("Не найдено")) return@withContext raw

        val altPrefix = if (isModel) "channels" else "models"
        val rawAlt = readHtmlFromURLDirect("$urlStart/$altPrefix/$cleanSlug")
        if (rawAlt.isNotBlank() && !rawAlt.contains("Не найдено")) return@withContext rawAlt

        val rawProfile = readHtmlFromURLDirect("$urlStart/profiles/$cleanSlug")
        if (rawProfile.isNotBlank() && !rawProfile.contains("Не найдено")) return@withContext rawProfile

        readHtmlFromURLDirect("$urlStart/$cleanSlug")
    }

    /**
     * Выполняет первичную загрузку шапки из HTML и нулевой страницы видео через JSON API.
     */
    fun loadInitial() {
        if (cleanSlug.isBlank()) {
            uiState = uiState.copy(
                isLoadingInitial = false,
                error = "Некорректный адрес профиля",
            )
            return
        }

        initialJob?.cancel()
        pageJobs.values.forEach { it.cancel() }
        pageJobs.clear()
        currentPage = 0
        pagesCache.clear()
        loadingPages.clear()
        errorPages.clear()
        gridStates.clear()

        uiState = uiState.copy(
            isLoadingInitial = true,
            isLoadingMore = false,
            isEndReached = false,
            error = null,
            videos = emptyList(),
        )

        initialJob = screenModelScope.launch {
            try {
                // 1. Загрузка шапки профиля из HTML страницы канала / модели (только при первом открытии)
                val currentHeader = uiState.header
                val parsedHeader = if (currentHeader.availableModels.isNotEmpty() || currentHeader.bannerUrl.isNotBlank() || currentHeader.hasAboutMe) {
                    currentHeader
                } else {
                    val headerHtml = fetchChannelHtml(cleanSlug, pathPrefix, isModel)
                    withContext(Dispatchers.Default) {
                        parserChannelHeader(
                            html = headerHtml,
                            fallbackSlug = cleanSlug,
                            fallbackName = initialModel?.name.orEmpty(),
                            fallbackSubscribers = initialModel?.count.orEmpty(),
                            isModel = isModel,
                        )
                    }
                }

                // 1.5. Загрузка рейтингов автора / модели из JSON API сайта (/profiles/{slug}/ranks/straight)
                val headerWithRanks = if (parsedHeader.rankings.isNotEmpty()) {
                    parsedHeader
                } else {
                    try {
                        val ranksJson = withContext(Dispatchers.IO) {
                            readHtmlFromURLDirect("$urlStart/profiles/$cleanSlug/ranks/straight")
                        }
                        val ranks = withContext(Dispatchers.Default) {
                            parserChannelRanksJson(ranksJson)
                        }
                        if (ranks.isNotEmpty()) parsedHeader.copy(rankings = ranks) else parsedHeader
                    } catch (_: Exception) {
                        parsedHeader
                    }
                }

                // 2. Загрузка 0-й страницы видео из JSON API
                val effectivePrefix = if (headerWithRanks.isModel) "models" else "channels"
                var jsonVideos = fetchVideosJson(0, overridePrefix = effectivePrefix)
                if (jsonVideos.isBlank() || jsonVideos.trim() == "{\"videos\":[]}") {
                    val altPrefix = if (effectivePrefix == "models") "channels" else "models"
                    val altJson = fetchVideosJson(0, overridePrefix = altPrefix)
                    if (altJson.isNotBlank() && altJson.trim() != "{\"videos\":[]}") {
                        jsonVideos = altJson
                    }
                }

                val result = withContext(Dispatchers.Default) {
                    parserChannelVideosResult(jsonVideos)
                }

                pagesCache[0] = result.videos
                uiState = uiState.copy(
                    header = headerWithRanks,
                    videos = result.videos,
                    totalVideosCount = result.totalVideos ?: 0,
                    currentPage = 0,
                    isLoadingInitial = false,
                    isEndReached = result.videos.isEmpty(),
                    error = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "ScreenX_ChannelSM: ошибка загрузки %s (%s)", cleanSlug, pathPrefix)
                uiState = uiState.copy(
                    isLoadingInitial = false,
                    error = "Не удалось загрузить данные профиля",
                )
            }
        }
    }

    /**
     * Загружает конкретную страницу [page] (0-based) в [pagesCache].
     */
    fun loadPage(page: Int) {
        val targetPage = page.coerceAtLeast(0)
        if (pagesCache.containsKey(targetPage) || loadingPages.contains(targetPage)) return

        loadingPages.add(targetPage)
        errorPages.remove(targetPage)

        val job = screenModelScope.launch {
            try {
                val effectivePrefix = if (uiState.header.isModel) "models" else "channels"
                var jsonVideos = fetchVideosJson(targetPage, overridePrefix = effectivePrefix)
                if (jsonVideos.isBlank() || jsonVideos.trim() == "{\"videos\":[]}") {
                    val altPrefix = if (effectivePrefix == "models") "channels" else "models"
                    val altJson = fetchVideosJson(targetPage, overridePrefix = altPrefix)
                    if (altJson.isNotBlank() && altJson.trim() != "{\"videos\":[]}") {
                        jsonVideos = altJson
                    }
                }

                val result = withContext(Dispatchers.Default) {
                    parserChannelVideosResult(jsonVideos)
                }

                pagesCache[targetPage] = result.videos
                loadingPages.remove(targetPage)
                if (result.totalVideos != null && result.totalVideos > 0) {
                    uiState = uiState.copy(totalVideosCount = result.totalVideos)
                }

                // Ограничение кэша в памяти: если сохранено > 20 страниц, освобождаем удалённые от текущей
                if (pagesCache.size > 20) {
                    val farPages = pagesCache.keys.filter { kotlin.math.abs(it - targetPage) > 8 }
                    farPages.forEach { farPage ->
                        pagesCache.remove(farPage)
                        gridStates.remove(farPage)
                    }
                }
            } catch (e: CancellationException) {
                loadingPages.remove(targetPage)
                throw e
            } catch (e: Exception) {
                Timber.e(e, "ScreenX_ChannelSM: сбой загрузки страницы %d для %s", targetPage, cleanSlug)
                loadingPages.remove(targetPage)
                errorPages[targetPage] = "Не удалось загрузить страницу ${targetPage + 1}"
            } finally {
                pageJobs.remove(targetPage)
            }
        }
        pageJobs[targetPage] = job
    }

    /**
     * Повторяет загрузку страницы после ошибки.
     */
    fun retryPage(page: Int) {
        errorPages.remove(page)
        loadPage(page)
    }

    /**
     * Переход на конкретную страницу видеороликов автора ([page], 0-based).
     */
    fun goToPage(page: Int) {
        val targetPage = page.coerceAtLeast(0)
        currentPage = targetPage
        loadPage(targetPage)
    }

    /**
     * Переход на следующую страницу.
     */
    fun loadNextPage() {
        if (currentPage < uiState.maxPages - 1) {
            goToPage(currentPage + 1)
        }
    }

    /**
     * Переход на предыдущую страницу.
     */
    fun loadPrevPage() {
        if (currentPage > 0) {
            goToPage(currentPage - 1)
        }
    }

    /**
     * Переключает режим сортировки («Свежие», «Новые», «Топ») и перезагружает ленту.
     */
    fun changeSort(newSort: ChannelSortOrder) {
        if (uiState.currentSort == newSort) return
        uiState = uiState.copy(currentSort = newSort)
        loadInitial()
    }

    /**
     * Выбирает модель для фильтрации видео (или `null` для сброса фильтра).
     */
    fun selectModel(model: ChannelModelFilterItem?) {
        if (uiState.selectedModel == model) {
            uiState = uiState.copy(isModelFilterExpanded = false)
            return
        }
        uiState = uiState.copy(
            selectedModel = model,
            modelFilterQuery = "",
            isModelFilterExpanded = false,
        )
        loadInitial()
    }

    /**
     * Обновляет поисковый запрос внутри выпадающего списка моделей.
     */
    fun onModelFilterQueryChange(query: String) {
        uiState = uiState.copy(modelFilterQuery = query)
    }

    /**
     * Управляет видимостью выпадающего списка моделей.
     */
    fun setModelFilterExpanded(expanded: Boolean) {
        uiState = uiState.copy(isModelFilterExpanded = expanded)
    }

    /**
     * Сбрасывает выбранную модель и возвращает общий список видеороликов.
     */
    fun clearModelFilter() {
        selectModel(null)
    }
}

/**
 * Hilt-модуль привязки фабрики [ScreenX_ChannelSM.Factory].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleChannel {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenX_ChannelSM.Factory::class)
    abstract fun bindChannelScreenModelFactory(
        factory: ScreenX_ChannelSM.Factory,
    ): ScreenModelFactory
}
