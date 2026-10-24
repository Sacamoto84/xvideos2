package com.client.xvideos.x.feature.rubric

import com.client.xvideos.x.feature.net.fetchHtmlInOwnSession
import com.client.xvideos.x.model.XCategory
import com.client.xvideos.x.model.XRubric
import com.client.xvideos.x.parcer.parserRubricMenu
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.Collator
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Меню категорий сайта X по ориентациям, с кешем в памяти на процесс.
 *
 * Меню берётся запросом в отдельной сессии ([fetchHtmlInOwnSession]): ради
 * него ориентация сессии сайта переключается, и в общем клиенте это
 * переключило бы пользователю ленту и сессию входа. Категории меняются редко,
 * поэтому список ориентации грузится один раз; ошибка не кешируется.
 */
@Singleton
class XRubricCatalog internal constructor(
    private val fetch: suspend (String) -> String,
) {
    @Inject
    constructor() : this(::fetchHtmlInOwnSession)

    private val lists = ConcurrentHashMap<XCategory, ImmutableList<XRubric>>()

    /** Уже загруженный список ориентации [orientation] или `null`. */
    fun cached(orientation: XCategory): ImmutableList<XRubric>? = lists[orientation]

    /**
     * Категории ориентации [orientation] по алфавиту подписи.
     *
     * @throws IOException нет сети, ошибка сервера или меню не разобрано.
     */
    suspend fun load(orientation: XCategory): ImmutableList<XRubric> {
        lists[orientation]?.let { return it }
        val html = fetch(buildRubricMenuUrl(orientation))
        val rubrics = withContext(Dispatchers.Default) {
            val collator = Collator.getInstance(Locale.forLanguageTag("ru"))
            parserRubricMenu(html).sortedWith { a, b -> collator.compare(a.title, b.title) }
        }
        // У сайта категории есть всегда: пустое меню — поменялась разметка.
        if (rubrics.isEmpty()) throw IOException("Меню категорий не разобрано")
        return rubrics.toImmutableList().also { lists[orientation] = it }
    }
}
