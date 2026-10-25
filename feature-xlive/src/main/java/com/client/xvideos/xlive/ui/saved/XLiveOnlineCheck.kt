package com.client.xvideos.xlive.ui.saved

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException

/** Сколько комнат проверять одновременно. */
private const val PARALLEL_CHECKS = 6

/** Сколько ждать ответа по одной комнате: зависший запрос не должен держать всю проверку. */
private const val CHECK_TIMEOUT_MS = 10_000L

/**
 * Кто из сохранённых комнат в эфире. «Мои» и «История» данных об эфире не
 * хранят — сайт спрашивается по каждой комнате.
 *
 * Ответы копятся и публикуются разом, когда проверка закончилась: «Мои»
 * сортируются по эфиру, и карточки, всплывающие по одной, прыгали бы по сетке
 * всё время, пока идут ответы.
 *
 * Проверка одна за раз. Вызов во время проверки запоминается — последний,
 * — и выполняется, когда она закончится: переход из «Моих» в «Историю» во
 * время проверки не оставит «Историю» непроверенной.
 *
 * @param isOnline источник статуса по нику; подменяется в тестах.
 */
class XLiveOnlineCheck(
    private val scope: CoroutineScope,
    private val isOnline: suspend (String) -> Boolean,
) {

    private val _online = MutableStateFlow<PersistentMap<String, Boolean>>(persistentMapOf())

    /** Ник → в эфире. Комнаты без ответа здесь нет. */
    val online: StateFlow<ImmutableMap<String, Boolean>> = _online.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private var job: Job? = null
    private var queued: List<String>? = null

    /** Проверить [names]; уже известные — только при [force]: свайп вниз. */
    fun check(names: List<String>, force: Boolean = false) {
        if (job?.isActive == true) {
            queued = names
            return
        }
        val known = _online.value
        val todo = names.distinct().filter { force || it !in known }
        if (todo.isEmpty()) return
        _isChecking.value = true
        job = scope.launch {
            val gate = Semaphore(PARALLEL_CHECKS)
            val answers = todo
                .map { name -> async { gate.withPermit { name to statusOf(name) } } }
                .awaitAll()
                .mapNotNull { (name, status) -> status?.let { name to it } }
            Timber.i(
                "XLive: эфир проверен — в эфире %d из %d, без ответа %d",
                answers.count { it.second }, todo.size, todo.size - answers.size,
            )
            _online.update { it.puttingAll(answers.toMap()) }
            _isChecking.value = false
            job = null
            queued?.let { next ->
                queued = null
                check(next)
            }
        }
    }

    private suspend fun statusOf(name: String): Boolean? = try {
        withTimeoutOrNull(CHECK_TIMEOUT_MS) { isOnline(name) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Timber.w("XLive: эфир не проверен — %s", e.javaClass.simpleName)
        null
    }
}
