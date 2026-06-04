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
 */
class LDownloadProgress(private val scope: CoroutineScope) {

    private val flow = MutableStateFlow(HIDDEN)
    val percentDownload: StateFlow<Float> = flow

    private val lock = Any()
    private val activeFileProgress = mutableMapOf<Int, Float>()
    private var activeJobs = 0
    private var totalFiles = 0
    private var finishedFiles = 0
    private var nextFileProgressId = 0

    fun begin(fileCount: Int) {
        synchronized(lock) {
            activeJobs += 1
            totalFiles += fileCount.coerceAtLeast(1)
            recompute()
        }
    }

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

    fun startFile(): Int = synchronized(lock) {
        val id = nextFileProgressId++
        activeFileProgress[id] = 0f
        recompute()
        id
    }

    fun updateFile(id: Int, fraction: Float) {
        synchronized(lock) {
            if (id in activeFileProgress) {
                activeFileProgress[id] = fraction.coerceIn(0f, 1f)
                recompute()
            }
        }
    }

    fun finishFile(id: Int) {
        synchronized(lock) {
            if (activeFileProgress.remove(id) != null) {
                finishedFiles = (finishedFiles + 1).coerceAtMost(totalFiles)
                recompute()
            }
        }
    }

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
        const val HIDDEN = -2f
        const val DONE = 1f
        const val DONE_VISIBLE_MS = 400L
    }
}
