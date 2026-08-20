package com.client.xvideos.r.common.search

import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.r.network.api.RedApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

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

    /**
     * Раньше здесь висел голый `searchText.collect { … сетевой запрос … }`:
     * запрос на каждое изменение текста, без паузы и без отмены предыдущего.
     * Сейчас цепочка ждёт [SUGGESTIONS_DEBOUNCE_MS] и отменяет незаконченный
     * запрос при новом вводе — за это отвечает `mapLatest`.
     */
    init {
        scope.launch {
            searchText
                .map { it.text }
                .distinctUntilChanged()
                .debounce(SUGGESTIONS_DEBOUNCE_MS)
                .mapLatest { text -> suggestionsFor(text) }
                .collect { searchTextSuggestions.value = it }
        }
    }

    private suspend fun suggestionsFor(text: String): List<SuggestionItem> {
        return try {
            // Пробел вместо пустой строки: на него API отдаёт подсказки по
            // умолчанию, а на пустой параметр — ничего.
            val request = text.ifEmpty { " " }

            redApi.getTagSuggestions(request)
                .onFailure { Timber.w(it, "!!! R_SearchExplorer: подсказки тегов не пришли") }
                .map { list -> list.map { s -> SuggestionItem(text = s.text, count = s.gifs) } }
                .getOrDefault(emptyList())
        } catch (e: CancellationException) {
            // Ввод продолжился — mapLatest отменил эту ветку штатно, ошибки нет.
            throw e
        } catch (e: Exception) {
            Timber.e("!!! SearchRed searchText ${e.localizedMessage}")
            emptyList()
        }
    }
}
