package com.client.xvideos.x.screens.actresses

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.x.feature.net.fetchHtml
import com.client.xvideos.x.feature.net.notFoundAsEmpty
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
import com.client.xvideos.x.model.ActressesIndexDropdownType
import com.client.xvideos.x.model.ActressesIndexFilterOption
import com.client.xvideos.x.model.ActressesIndexUiState
import com.client.xvideos.x.normalizeXUrl
import com.client.xvideos.x.parcer.parseActressesIndexPage
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
 * ScreenModel экрана каталога актрис, моделей и рейтингов (`ScreenX_ActressesIndex`).
 *
 * Управляет загрузкой страниц каталога из HTML, бесконечной пагинацией,
 * фильтрацией по странам, типам моделей и периодам сортировки.
 */
@Stable
class ScreenX_ActressesIndexSM @AssistedInject constructor(
    @Assisted("urlPath") val initialUrlPath: String,
    @Assisted("initialTitle") val initialTitle: String = "",
) : ScreenModel {

    /** Текущий путь или URL каталога (например, `"/porn-actresses-index/from/russia/ever"`). */
    var currentUrlPath: String by mutableStateOf(initialUrlPath)
        private set

    /** Текущее состояние экрана каталога. */
    var uiState: ActressesIndexUiState by mutableStateOf(
        ActressesIndexUiState(
            currentUrlPath = initialUrlPath,
            title = initialTitle,
            isLoadingInitial = true,
        )
    )
        private set

    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(
            @Assisted("urlPath") urlPath: String,
            @Assisted("initialTitle") initialTitle: String = "",
        ): ScreenX_ActressesIndexSM
    }

    private var initialJob: Job? = null
    private var pagingJob: Job? = null
    private var currentPageIndex: Int = 0

    init {
        loadInitial()
    }

    /**
     * Загружает начальную страницу каталога по текущему [currentUrlPath].
     */
    fun loadInitial() {
        initialJob?.cancel()
        pagingJob?.cancel()
        currentPageIndex = 0

        uiState = uiState.copy(
            isLoadingInitial = true,
            isLoadingMore = false,
            isEndReached = false,
            error = null,
            loadMoreError = null,
            items = emptyList(),
            activeDropdown = null,
            dropdownSearchQuery = "",
        )

        initialJob = screenModelScope.launch {
            try {
                val targetUrl = normalizeXUrl(currentUrlPath)
                val html = withContext(Dispatchers.IO) {
                    readHtmlFromURLDirect(targetUrl)
                }

                if (html.isBlank()) {
                    uiState = uiState.copy(
                        isLoadingInitial = false,
                        error = "Не удалось загрузить данные каталога",
                    )
                    return@launch
                }

                val catalog = withContext(Dispatchers.Default) {
                    parseActressesIndexPage(html)
                }

                currentPageIndex = catalog.currentPage
                val displayTitle = catalog.totalCountTitle.ifBlank {
                    initialTitle.ifBlank { "Каталог актрис" }
                }

                uiState = uiState.copy(
                    currentUrlPath = currentUrlPath,
                    title = displayTitle,
                    catalog = catalog,
                    items = catalog.items,
                    isLoadingInitial = false,
                    isEndReached = !catalog.hasNextPage || catalog.items.isEmpty(),
                    error = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "ScreenX_ActressesIndexSM: ошибка загрузки каталога %s", currentUrlPath)
                uiState = uiState.copy(
                    isLoadingInitial = false,
                    error = "Не удалось загрузить каталог актрис",
                )
            }
        }
    }

    /**
     * Загружает следующую страницу каталога при бесконечной прокрутке.
     */
    fun loadNextPage() {
        // После сбоя — только по «Повторить» (retryNextPage): прокрутка внизу списка
        // иначе повторяла запрос сразу после каждой ошибки, без паузы.
        if (!uiState.canLoadMore) return

        pagingJob?.cancel()
        val nextPage = currentPageIndex + 1

        val nextPagePath = if (uiState.catalog.nextPageUrl.isNotBlank()) {
            uiState.catalog.nextPageUrl
        } else {
            val basePath = currentUrlPath.trimEnd('/').replace(Regex("/\\d+$"), "")
            "$basePath/$nextPage"
        }

        uiState = uiState.copy(isLoadingMore = true)

        pagingJob = screenModelScope.launch {
            try {
                val targetUrl = normalizeXUrl(nextPagePath)
                // Сбой сети — исключение: список не помечается законченным, внизу появляется
                // «Повторить» (loadMoreError). 404 — каталог действительно кончился.
                val html = withContext(Dispatchers.IO) {
                    notFoundAsEmpty { fetchHtml(targetUrl) }
                }

                val parsedCatalog = withContext(Dispatchers.Default) {
                    parseActressesIndexPage(html)
                }

                val newItems = parsedCatalog.items
                if (newItems.isEmpty()) {
                    uiState = uiState.copy(
                        isLoadingMore = false,
                        isEndReached = true,
                    )
                } else {
                    currentPageIndex = nextPage
                    val existingSlugs = uiState.items.map { it.slug }.toSet()
                    val distinctNew = newItems.filter { it.slug !in existingSlugs }
                    uiState = uiState.copy(
                        catalog = parsedCatalog,
                        items = uiState.items + distinctNew,
                        isLoadingMore = false,
                        isEndReached = !parsedCatalog.hasNextPage || distinctNew.isEmpty(),
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "ScreenX_ActressesIndexSM: сбой подгрузки следующей страницы %s", nextPagePath)
                uiState = uiState.copy(
                    isLoadingMore = false,
                    loadMoreError = "Не удалось загрузить следующую страницу",
                )
            }
        }
    }

    /**
     * Повтор подгрузки следующей страницы после сбоя.
     */
    fun retryNextPage() {
        uiState = uiState.copy(loadMoreError = null)
        loadNextPage()
    }

    /**
     * Выбирает опцию фильтра (страна, тип модели или период) и перезагружает каталог.
     */
    fun selectFilterOption(option: ActressesIndexFilterOption) {
        if (option.urlPath.isBlank() || option.urlPath == currentUrlPath) {
            closeDropdown()
            return
        }
        currentUrlPath = option.urlPath
        closeDropdown()
        loadInitial()
    }

    /**
     * Открывает или закрывает выпадающее меню фильтра.
     */
    fun toggleDropdown(type: ActressesIndexDropdownType) {
        uiState = if (uiState.activeDropdown == type) {
            uiState.copy(activeDropdown = null, dropdownSearchQuery = "")
        } else {
            uiState.copy(activeDropdown = type, dropdownSearchQuery = "")
        }
    }

    /**
     * Закрывает текущее открытое выпадающее меню.
     */
    fun closeDropdown() {
        uiState = uiState.copy(activeDropdown = null, dropdownSearchQuery = "")
    }

    /**
     * Обновляет поисковый запрос внутри выпадающего списка фильтра (например, поиск страны).
     */
    fun onDropdownSearchQueryChange(query: String) {
        uiState = uiState.copy(dropdownSearchQuery = query)
    }
}

/**
 * Hilt-модуль привязки фабрики [ScreenX_ActressesIndexSM.Factory].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleActressesIndex {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenX_ActressesIndexSM.Factory::class)
    abstract fun bindActressesIndexScreenModelFactory(
        factory: ScreenX_ActressesIndexSM.Factory,
    ): ScreenModelFactory
}
