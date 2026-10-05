package com.client.xvideos.x.screens.tags

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.hilt.ScreenModelFactory
import cafe.adriel.voyager.hilt.ScreenModelFactoryKey
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.ModelScreenTag
import com.client.xvideos.x.parcer.parserScreenTags
import com.client.xvideos.x.screens.common.PageCache
import com.client.xvideos.x.urlStart
import com.client.xvideos.x.feature.net.readHtmlFromURLDirect
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import timber.log.Timber

/**
 * ScreenModel экрана выдачи по конкретному тегу/категории раздела X.
 *
 * Выполняет асинхронную подгрузку и разбор HTML страниц тега ([loadPage]),
 * поддерживает пагинацию и форматирование имени тега для формирования корректного URL.
 *
 * @property tag Исходное имя тега.
 */
@Stable
class ScreenTagsViewModel @AssistedInject constructor(
    @Assisted val tag: String,
) : ScreenModel {

    /** Фабрика assisted injection для передачи параметра [tag]. */
    @AssistedFactory
    interface Factory : ScreenModelFactory {
        fun create(tag: String): ScreenTagsViewModel
    }

    /** Compose-состояние экрана тега с заголовком и списком роликов. */
    var screen by mutableStateOf(ModelScreenTag("", "", emptyList()))
        private set

    /**
     * Загруженные страницы: возврат к странице в пейджере не грузит её заново.
     * Объявлены до [init]: он сразу вызывает [loadPage].
     */
    private val pages = PageCache<ModelScreenTag>(maxPages = MAX_CACHED_PAGES)

    init {
        // Раньше здесь был runBlocking { readHtmlFromURLDirect(...) } — сетевой запрос
        // блокировал поток создания ScreenModel (UI-поток) → ANR на медленной сети.
        // Теперь грузим в screenModelScope, а тяжёлый парсинг уводим на Dispatchers.Default.
        screenModelScope.launch {
            // Непойманное исключение здесь роняет приложение целиком: это launch
            // без обработчика. Заголовок и число страниц не настолько важны,
            // чтобы платить за них падением — сами страницы грузит пейджер.
            try {
                screen = loadPage(0)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w("!!! Заголовок тега %s не загрузился: %s", tag, e.javaClass.simpleName)
            }
        }
    }

    /** Ролики уже загруженной страницы [index] или `null` — страницу надо грузить. */
    fun cachedItems(index: Int): List<ItemsX>? = pages[index]?.items

    /**
     * Разбор страницы выдачи с номером [index] (считается с нуля).
     *
     * Загруженная страница берётся из памяти. Нулевая страница может прийти
     * дважды: в [init] ради заголовка и числа страниц и первой страницей
     * пейджера, если он попросил её раньше, чем закончилась первая загрузка.
     *
     * @param index Номер страницы пагинации (0, 1, 2, ...).
     * @return Модель разобранной страницы [ModelScreenTag].
     */
    suspend fun loadPage(index: Int): ModelScreenTag {
        pages[index]?.let { return it }
        // Страницы адресуются /tags/<тег>/N; /tags/<тег> и /tags/<тег>/0 — одно и то же.
        // Названия тегов парсятся с пробелами ("big tits"), а в URL XVideos использует дефисы ("big-tits").
        // Также санитизируем спецсимволы путей/запросов (#, ?, &, /, \), чтобы не ломать адрес запроса.
        val formattedTag = sanitizeTagForUrl(tag)
        if (formattedTag.isEmpty()) {
            throw IOException("Недопустимое имя тега: $tag")
        }
        val html = readHtmlFromURLDirect("$urlStart/tags/$formattedTag/$index")
        if (html.isEmpty()) {
            throw IOException("Не удалось загрузить страницу тега $tag")
        }
        val result = withContext(Dispatchers.Default) { parserScreenTags(html) }
        if (index == 0 && (screen.lastPage <= 1 || screen.title0.isEmpty())) {
            screen = result
        }
        pages[index] = result
        return result
    }

    private companion object {
        const val MAX_CACHED_PAGES = 20
    }
}

/**
 * Hilt-модуль привязки фабрики [ScreenTagsViewModel.Factory].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ScreenModuleTags {

    @Binds
    @IntoMap
    @ScreenModelFactoryKey(ScreenTagsViewModel.Factory::class)
    abstract fun bindScreenTagsScreenModel(
        hiltDetailsScreenModelFactory: ScreenTagsViewModel.Factory,
    ): ScreenModelFactory

}

private val SANITIZE_TAG_CHARS_REGEX = Regex("[#?&/\\\\\\s\\u00A0]+")
private val REPEATED_HYPHENS_REGEX = Regex("-+")

/**
 * Преобразует название тега в валидный URL-сегмент для XVideos:
 * - Заменяет пробельные символы (включая неразрывные \u00A0) и спецсимволы путей/запросов (#, ?, &, /, \) на дефисы.
 * - Схлопывает повторяющиеся дефисы и обрезает краевые.
 */
internal fun sanitizeTagForUrl(tag: String): String {
    return tag.trim()
        .replace(SANITIZE_TAG_CHARS_REGEX, "-")
        .replace(REPEATED_HYPHENS_REGEX, "-")
        .trim('-')
}
