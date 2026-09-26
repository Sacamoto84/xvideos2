package com.client.xvideos.l.featured.saved

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Общий помощник, который агрегирует прогресс параллельных загрузок (likes и collection)
 * и публикует одно значение в [percentDownload]:
 *  - [HIDDEN]  — индикатор скрыт;
 *  - 0..1f      — текущий прогресс;
 *  - [DONE]    — завершено, скоро скроется (через [DONE_VISIBLE_MS]).
 *
 * Один загрузочный «акт» (одно сохранение PicsDetails) оборачивается в [begin]/[finish].
 * Внутри него каждое скачивание файла оборачивается в [startFile]/[updateFile]/[finishFile].
 *
 * @param scope Область корутин для отложенного скрытия индикатора после завершения.
 */
class LDownloadProgress(private val scope: CoroutineScope) {

    private val flow = MutableStateFlow(HIDDEN)
    /** Поток совокупного процента скачивания (от 0f до 1f, либо [HIDDEN]). */
    val percentDownload: StateFlow<Float> = flow

    private val lock = Any()
    private val activeFileProgress = mutableMapOf<Int, Float>()
    private var activeJobs = 0
    private var totalFiles = 0
    private var finishedFiles = 0
    private var nextFileProgressId = 0

    /**
     * Регистрирует начало пакета скачивания на указанное количество файлов [fileCount].
     *
     * @param fileCount Ожидаемое количество файлов для загрузки.
     */
    fun begin(fileCount: Int) {
        synchronized(lock) {
            activeJobs += 1
            totalFiles += fileCount.coerceAtLeast(1)
            recompute()
        }
    }

    /**
     * Фиксирует завершение пакета скачивания. При окончании всех активных задач
     * переводит состояние в [DONE] и через [DONE_VISIBLE_MS] скрывает индикатор ([HIDDEN]).
     */
    fun finish() {
        val shouldHide: Boolean
        synchronized(lock) {
            activeJobs = (activeJobs - 1).coerceAtLeast(0)
            shouldHide = activeJobs == 0
            if (shouldHide) {
                activeFileProgress.clear()
                finishedFiles = totalFiles
                flow.value = DONE
                totalFiles = 0
                finishedFiles = 0
            } else {
                recompute()
            }
        }

        if (shouldHide) {
            scope.launch {
                delay(DONE_VISIBLE_MS)
                synchronized(lock) {
                    if (activeJobs == 0) {
                        flow.value = HIDDEN
                    }
                }
            }
        }
    }

    /**
     * Регистрирует начало скачивания отдельного файла.
     *
     * @return Уникальный идентификатор отслеживаемого файла для последующих обновлений.
     */
    fun startFile(): Int = synchronized(lock) {
        val id = nextFileProgressId++
        activeFileProgress[id] = 0f
        recompute()
        id
    }

    /**
     * Обновляет долю скачивания для файла с указанным [id].
     *
     * @param id Идентификатор файла, полученный из [startFile].
     * @param fraction Прогресс от 0f до 1f.
     */
    fun updateFile(id: Int, fraction: Float) {
        synchronized(lock) {
            if (id in activeFileProgress) {
                activeFileProgress[id] = fraction.coerceIn(0f, 1f)
                recompute()
            }
        }
    }

    /**
     * Завершает скачивание файла с указанным [id] и увеличивает счетчик полностью скачанных файлов.
     *
     * @param id Идентификатор файла, полученный из [startFile].
     */
    fun finishFile(id: Int) {
        synchronized(lock) {
            if (activeFileProgress.remove(id) != null) {
                finishedFiles = (finishedFiles + 1).coerceAtMost(totalFiles)
                recompute()
            }
        }
    }

    /**
     * Пересчитывает суммарный прогресс всех загрузок и обновляет [flow].
     */
    private fun recompute() {
        if (totalFiles <= 0 || activeJobs <= 0) {
            flow.value = HIDDEN
            return
        }
        val activeProgress = activeFileProgress.values.sum()
        flow.value = ((finishedFiles + activeProgress) / totalFiles.toFloat())
            .coerceIn(0f, 1f)
    }

    companion object {
        /** Маркер скрытого состояния индикатора прогресса. */
        const val HIDDEN = -2f
        /** Маркер 100% завершения загрузки. */
        const val DONE = 1f
        /** Задержка в миллисекундах перед скрытием завершенного индикатора прогресса. */
        const val DONE_VISIBLE_MS = 400L
    }
}
