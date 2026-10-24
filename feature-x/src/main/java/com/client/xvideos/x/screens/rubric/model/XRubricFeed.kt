package com.client.xvideos.x.screens.rubric.model

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.ui.scroll.LazyListPosition
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ModelScreenTag
import com.client.xvideos.x.parcer.parserScreenTags
import com.client.xvideos.x.screens.common.PageCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private const val MAX_CACHED_PAGES = 20

/**
 * Выдача категории `/c/<slug>-<id>`: страницы, их кеш и прокрутка, фильтры.
 *
 * Отдельно от ScreenModel, чтобы проверяться без Android. Смена фильтров —
 * другая выдача: кеш, позиции и страница пейджера сбрасываются. Ответ,
 * запрошенный со старыми фильтрами, в новую выдачу не попадает.
 *
 * @param fetch HTML страницы по адресу; пустая строка — запрос не удался.
 */
@Stable
class XRubricFeed(
    private val id: Int,
    private val slug: String,
    private val fetch: suspend (String) -> String,
) {
    var filters by mutableStateOf(XRubricFilters())
        private set

    /** Заголовок и число страниц — со страницы 0 текущих фильтров. */
    var screen by mutableStateOf(ModelScreenTag())
        private set

    /** Загруженные страницы текущих фильтров; смена фильтров заменяет кеш целиком. */
    private var pages = PageCache<ModelScreenTag>(maxPages = MAX_CACHED_PAGES)

    /**
     * Позиции страниц пейджера — здесь, а не в композиции: экран уходит из неё
     * при переходе в плеер. Как у экрана тега.
     */
    val listPositions: MutableMap<Int, LazyListPosition> = HashMap()

    /** Сколько видео в ряду было у страницы при прокрутке; 0 — страницу ещё не показывали. */
    val shownPerRow: MutableMap<Int, MutableIntState> = HashMap()

    /** Страница пейджера. */
    var currentPage: Int = 0

    /** Ролики уже загруженной страницы [index] или `null` — страницу надо грузить. */
    fun cachedItems(index: Int): List<ItemsX>? = pages[index]?.items

    /**
     * Ставит фильтры [new]. Те же фильтры — ничего не трогает и возвращает `false`;
     * другие — сбрасывает выдачу на первую страницу и возвращает `true`.
     */
    fun applyFilters(new: XRubricFilters): Boolean {
        if (new == filters) return false
        filters = new
        pages = PageCache(maxPages = MAX_CACHED_PAGES)
        listPositions.clear()
        shownPerRow.clear()
        currentPage = 0
        screen = ModelScreenTag()
        return true
    }

    /**
     * Разбор страницы [index] (с нуля) текущих фильтров.
     *
     * @throws IOException сайт не ответил.
     */
    suspend fun loadPage(index: Int): ModelScreenTag {
        val requested = filters
        val cache = pages
        cache[index]?.let { return it }
        val html = fetch(buildRubricPageUrl(id, slug, index, requested))
        if (html.isEmpty()) throw IOException("Не удалось загрузить страницу категории")
        val result = withContext(Dispatchers.Default) { parserScreenTags(html) }
        // Пока страница грузилась, фильтры могли смениться: тогда ответ уходит
        // в выброшенный кеш, а заголовок новой выдачи не трогается.
        val headerMissing = screen.lastPage <= 1 || screen.title0.isEmpty()
        if (requested == filters && index == 0 && headerMissing) screen = result
        // Страница без роликов в кэш не идёт: «Повторить» должен идти в сеть.
        if (result.items.isNotEmpty()) cache[index] = result
        return result
    }
}
