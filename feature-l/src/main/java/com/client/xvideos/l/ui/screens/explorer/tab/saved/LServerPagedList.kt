package com.client.xvideos.l.ui.screens.explorer.tab.saved

import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.repository.toLUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Постраничный список с сервера L: первая загрузка, подгрузка следующей
 * страницы и обновление. Один на вкладки «лайки на сервере» и «подписки» —
 * раньше эти три операции были скопированы в оба экрана.
 *
 * Загрузка в каждый момент одна. Новая отменяет прежнюю и забирает оба
 * признака себе, поэтому обновление не гоняется с подгрузкой, а оборванная
 * загрузка не оставляет признак поднятым.
 *
 * Страницы сервер считает по номеру, а выдача под ними движется: лайк, снятый
 * здесь, сдвигает её к началу, поставленный в другом месте — к концу. Поэтому
 * после удаления подгрузка возвращается к уже запрошенным страницам, а
 * пришедшее сверяется с показанным по ключу. Раньше страницы шли подряд и
 * дописывались как есть: после удаления элемент на границе страниц
 * пропускался, после добавления — приходил второй раз, и сетка с ключом по id
 * падала на повторе.
 *
 * @param scope Область экрана: в ней идут загрузки.
 * @param loadPage Запрос страницы с номером от единицы.
 * @param keyOf Ключ элемента: по нему узнаётся уже показанный.
 * @param notifyNextPageFailed Сообщение о сбое подгрузки следующей страницы.
 * @param onReplaced Список заменён загрузкой: экран пересобирает то, что от него зависит.
 */
internal class LServerPagedList<T>(
    private val scope: CoroutineScope,
    private val loadPage: suspend (page: Int) -> Result<List<T>>,
    private val keyOf: (T) -> Any? = { it },
    private val notifyNextPageFailed: (String) -> Unit = SnackBar::error,
    private val onReplaced: (List<T>) -> Unit = {},
) {
    private val _items = MutableStateFlow<List<T>>(emptyList())
    val items: StateFlow<List<T>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    /** Идёт первая загрузка или подгрузка следующей страницы. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    /** Идёт обновление с первой страницы (pull-to-refresh). */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /** Текст сбоя первой загрузки или обновления; `null`, когда список в порядке. */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _nextPageFailed = MutableStateFlow(false)
    /**
     * Подгрузка следующей страницы не удалась и сама не повторится.
     *
     * Экран зовёт подгрузку, пока пользователь у конца списка и загрузки нет.
     * Без этого признака сбой сразу давал новый запрос: без сети запросы и
     * сообщения шли по кругу. Снимает его [onListEndLeft] или обновление.
     */
    val nextPageFailed: StateFlow<Boolean> = _nextPageFailed.asStateFlow()

    /** Есть ли ещё страницы. */
    var hasMore: Boolean = true
        private set

    /** Номер страницы, которую запросит следующая подгрузка. */
    private var nextPage = FIRST_PAGE + 1

    /** Размер первой страницы: по нему считается, на сколько страниц удаления сдвинули выдачу. */
    private var firstPageSize = 0

    /** Сколько элементов убрано через [removeIf] с прошлой подгрузки. */
    private var removedSinceLoad = 0

    /**
     * Удаление пришло, пока шла загрузка. Страница, запрошенная до него, могла
     * прийти уже сдвинутой: её первый прежний элемент переехал в конец
     * предыдущей страницы. Отступить тогда нужно на страницу больше, иначе он
     * не появится до обновления списка.
     */
    private var removedWhileLoading = false

    /**
     * Ключи убранных через [removeIf] до следующей полной загрузки. Страница,
     * запрошенная повторно, может ещё содержать убранное — сервер отстал, — и в
     * список оно возвращаться не должно.
     */
    private val removedKeys = HashSet<Any?>()
    private var loadJob: Job? = null

    /** Первая загрузка или повтор после её сбоя. */
    fun loadInitial() {
        if (_isLoading.value) return
        _errorMessage.value = null
        launchLoad(refreshing = false) { reload() }
    }

    /** Перезагрузка с первой страницы. */
    fun refresh() {
        if (_isRefreshing.value) return
        launchLoad(refreshing = true) { reload() }
    }

    /** Подгрузка следующей страницы; вызывается экраном у конца списка. */
    fun loadNextPage() {
        val busy = _isLoading.value || _isRefreshing.value
        val failed = _errorMessage.value != null || _nextPageFailed.value
        if (busy || failed || !hasMore) return
        launchLoad(refreshing = false) {
            // Страница из одних повторов на экране ничего не меняет, поэтому за ней
            // сразу запрашивается следующая. Предел — чтобы один заход не перебирал
            // выдачу без конца, если сервер отвечает одним и тем же.
            repeat(MAX_PAGES_PER_LOAD) {
                // Перед каждым запросом, а не один раз: удаление может прийти и
                // пока идёт запрос этого же захода.
                rewindForRemoved()
                val page = nextPage
                val list = request(page).getOrElse { error ->
                    Timber.e("LServerPagedList: next page $page failed: ${error.javaClass.simpleName}")
                    _nextPageFailed.value = true
                    notifyNextPageFailed(error.toLUserMessage())
                    return@launchLoad
                }
                if (appendPage(page, list)) return@launchLoad
            }
        }
    }

    /** Пользователь ушёл от конца списка: когда вернётся, подгрузку можно пробовать снова. */
    fun onListEndLeft() {
        _nextPageFailed.value = false
    }

    /**
     * Убирает элементы из списка без [onReplaced]: экран уже убрал их у себя сам.
     *
     * Зовётся после того, как элемент убран и на сервере: выдача стала короче,
     * и всё, что шло за ним, сдвинулось к началу.
     */
    fun removeIf(predicate: (T) -> Boolean) {
        val (removed, kept) = _items.value.partition(predicate)
        removed.mapTo(removedKeys) { keyOf(it) }
        removedSinceLoad += removed.size
        if (removed.isNotEmpty() && loadJob?.isActive == true) removedWhileLoading = true
        _items.value = kept
    }

    /**
     * Убранное сдвинуло выдачу к началу: первые ещё не показанные элементы лежат
     * теперь на уже запрошенных страницах — с них подгрузка и продолжается.
     */
    private fun rewindForRemoved() {
        if (removedSinceLoad == 0) return
        val pageSize = firstPageSize.coerceAtLeast(1)
        val shiftedPages = (removedSinceLoad + pageSize - 1) / pageSize + if (removedWhileLoading) 1 else 0
        nextPage = (nextPage - shiftedPages).coerceAtLeast(FIRST_PAGE)
        removedSinceLoad = 0
        removedWhileLoading = false
    }

    /**
     * Добавляет в список новое со страницы [page].
     *
     * Новый элемент встаёт за своим соседом по странице, если тот уже показан,
     * иначе — в конец. Обычная страница так дописывается целиком; страница,
     * запрошенная повторно, приносит элемент из середины выдачи, и в конце
     * списка он стоял бы не на своём месте.
     *
     * @return `false` — нового на странице нет: оно лежит дальше. Конец выдачи —
     * только пустая страница, как и раньше.
     */
    private fun appendPage(page: Int, list: List<T>): Boolean {
        if (list.isEmpty()) {
            hasMore = false
            return true
        }
        nextPage = page + 1
        val merged = _items.value.toMutableList()
        val knownKeys = merged.mapTo(HashSet()) { keyOf(it) }
        var insertAt = merged.size
        var added = false
        for (item in list) {
            val key = keyOf(item)
            when {
                key in removedKeys -> Unit
                key in knownKeys -> insertAt = merged.indexOfFirst { keyOf(it) == key } + 1
                else -> {
                    knownKeys += key
                    merged.add(insertAt++, item)
                    added = true
                }
            }
        }
        if (!added) return false
        publish(merged)
        return true
    }

    private suspend fun reload() {
        request(FIRST_PAGE)
            .onSuccess { list ->
                nextPage = FIRST_PAGE + 1
                firstPageSize = list.size
                removedSinceLoad = 0
                removedWhileLoading = false
                removedKeys.clear()
                hasMore = list.isNotEmpty()
                _errorMessage.value = null
                _nextPageFailed.value = false
                publish(list.distinctBy(keyOf))
            }
            .onFailure { error ->
                Timber.e("LServerPagedList: first page failed: ${error.javaClass.simpleName}")
                _errorMessage.value = error.toLUserMessage()
            }
    }

    private fun publish(list: List<T>) {
        _items.value = list
        onReplaced(list)
    }

    /** Непредвиденное исключение — такой же отказ, как ошибка сети: иначе оно уронило бы область экрана. */
    private suspend fun request(page: Int): Result<List<T>> = try {
        loadPage(page)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Запускает загрузку вместо прежней. Признаки ставятся здесь и снимаются по
     * завершению корутины, а не в её теле: отменённая до старта корутина тела
     * не выполняет, а исключение проскочило бы строку сброса.
     */
    private fun launchLoad(refreshing: Boolean, block: suspend () -> Unit) {
        loadJob?.cancel()
        _isRefreshing.value = refreshing
        _isLoading.value = !refreshing
        val job = scope.launch(start = CoroutineStart.LAZY) { block() }
        job.invokeOnCompletion {
            // Признаки уже принадлежат следующей загрузке — не трогаем.
            if (loadJob === job) {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
        loadJob = job
        job.start()
    }
}

private const val FIRST_PAGE = 1

/** Сколько страниц подряд один заход подгрузки запрашивает, пока не найдёт новое. */
private const val MAX_PAGES_PER_LOAD = 3
