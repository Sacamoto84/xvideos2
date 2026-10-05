package com.client.xvideos.l.featured.saved

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Очередь мутаций сохранённого в L: действия идут строго по одному, в порядке
 * вызова.
 *
 * Раньше лайки и альбомы держали по одному `Job` на все действия, и каждое
 * новое отменяло предыдущее. Сохранение лайка качает файлы и длится долго —
 * следующий лайк обрывал его молча. У альбомов отмена приходилась между
 * записью файла и обновлением списка: альбом на диске есть, в списке его нет.
 * Очередь защищает от гонок так же, но ничего не теряет.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LMutationQueue(private val scope: CoroutineScope) {

    private val mutex = Mutex()

    // Один поток на запуск: корутины доходят до замка в порядке вызова, а замок
    // отдаёт их в порядке очереди. На общем пуле «лайк, затем сразу удалить»
    // могли дойти до замка в обратном порядке.
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)

    /** Ставит мутацию в очередь. */
    fun launch(block: suspend CoroutineScope.() -> Unit): Job =
        scope.launch(dispatcher) { mutex.withLock { block() } }
}
