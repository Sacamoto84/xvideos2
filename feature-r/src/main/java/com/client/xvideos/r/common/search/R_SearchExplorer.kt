package com.client.xvideos.r.common.search

import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Стейт-холдер поиска в разделе Explorer R.
 *
 * Управляет вводом поискового запроса, дебаунсом [SUGGESTIONS_DEBOUNCE_MS],
 * отменой устаревших запросов через `mapLatest` и получением тегов-подсказок из [RedApi.getTagSuggestions].
 *
 * @param scope Скоп приложения.
 * @param dao Хранилище истории поиска в разделе Explorer.
 * @param redApiIn Провайдер сетевого API (отложенная ленивая инициализация).
 */
@Singleton
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class R_SearchExplorer @Inject constructor(
    @ApplicationScope scope: CoroutineScope,
    dao: RSearchHistoryExplorerFileStore,
    redApiIn: Provider<RedApi>,
) : ISearchTemplate(scope, dao) {

    /**
     * `by lazy`, а не `= redApiIn.get()`.
     *
     * `Provider` внедряют, чтобы отложить создание зависимости; вызов `get()` в
     * инициализаторе поля отсрочку сразу же и убирал, то есть от `Provider`
     * оставалось одно имя. Теперь `RedApi` создаётся при первом обращении —
     * оно происходит в корутине ниже, уже после конструктора.
     */
    private val redApi: RedApi by lazy { redApiIn.get() }

    init {
        launchSuggestions(query = { it.text.trim() }, load = ::suggestionsFor)
    }

    /**
     * Запрашивает подсказки тегов у сервера R для автодополнения строки поиска.
     */
    private suspend fun suggestionsFor(text: String): List<SuggestionItem> {
        return try {
            // Пробел вместо пустой строки: на него API отдаёт подсказки по
            // умолчанию, а на пустой параметр — ничего.
            val request = text.ifEmpty { " " }

            redApi.getTagSuggestions(request)
                .onFailure { Timber.w("R_SearchExplorer: подсказки тегов не пришли: ${it.javaClass.simpleName}") }
                .map { list ->
                    if (list.isEmpty()) {
                        emptyList()
                    } else {
                        val result = ArrayList<SuggestionItem>(list.size)
                        for (s in list) {
                            result.add(SuggestionItem(text = s.text, count = s.gifs))
                        }
                        result
                    }
                }
                .getOrDefault(emptyList())
        } catch (e: CancellationException) {
            // Ввод продолжился — mapLatest отменил эту ветку штатно, ошибки нет.
            throw e
        } catch (e: Exception) {
            Timber.e("SearchRed searchText error: ${e.javaClass.simpleName}")
            emptyList()
        }
    }
}
