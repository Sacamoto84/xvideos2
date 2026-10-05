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
 * @param scope Область экрана: в ней идут загрузки.
 * @param loadPage Запрос страницы с номером от единицы.
 * @param notifyNextPageFailed Сообщение о сбое подгрузки следующей страницы.
 * @param onReplaced Список заменён загрузкой: экран пересобирает то, что от него зависит.
 */
internal class LServerPagedList<T>(
    private val scope: CoroutineScope,
    private val loadPage: suspend (page: Int) -> Result<List<T>>,
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

    private var currentPage = 1
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
            val nextPage = currentPage + 1
            request(nextPage)
                .onSuccess { list ->
                    if (list.isNotEmpty()) {
                        currentPage = nextPage
                        publish(_items.value + list)
                    } else {
                        hasMore = false
                    }
                }
                .onFailure { error ->
                    Timber.e("LServerPagedList: next page $nextPage failed: ${error.javaClass.simpleName}")
                    _nextPageFailed.value = true
                    notifyNextPageFailed(error.toLUserMessage())
                }
        }
    }

    /** Пользователь ушёл от конца списка: когда вернётся, подгрузку можно пробовать снова. */
    fun onListEndLeft() {
        _nextPageFailed.value = false
    }

    /** Убирает элементы из списка без [onReplaced]: экран уже убрал их у себя сам. */
    fun removeIf(predicate: (T) -> Boolean) {
        _items.value = _items.value.filterNot(predicate)
    }

    private suspend fun reload() {
        request(1)
            .onSuccess { list ->
                currentPage = 1
                hasMore = list.isNotEmpty()
                _errorMessage.value = null
                _nextPageFailed.value = false
                publish(list)
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
