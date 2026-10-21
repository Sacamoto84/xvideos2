package com.client.xvideos.x.feature.saved

import com.client.xvideos.common.kdownloader.utils.getTempPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/** Собирает картинку-сетку кадров ролика [video] в [out] (JPEG). `false` — собрать не удалось. */
fun interface StoryboardRenderer {
    suspend fun render(video: File, out: File): Boolean
}

/**
 * Сетки кадров скачанных роликов X: `<id>.storyboard.jpg` рядом с `<id>.mp4` в
 * каталоге загрузок [dir]. Сетка собирается один раз — после скачивания или
 * при первом открытии плеера — и дальше читается с диска.
 *
 * Сборка живёт в [scope] загрузок, а не в корутине того, кто её попросил:
 * закрытый посреди сборки плеер её не обрывает, и при следующем открытии сетка
 * готова. На ролик — одна сборка за раз. Рендер пишет во временный файл, и
 * готовым он становится переименованием — недособранная сетка готовой не
 * выглядит. Сетку удалённого ролика переименование не создаёт.
 */
class LocalStoryboards(
    private val dir: String,
    private val scope: CoroutineScope,
    private val renderer: StoryboardRenderer,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    /** Идущие сборки по id ролика; запись снимается, когда сборка закончилась. */
    private val builds = HashMap<Long, Deferred<File?>>()

    /** Файл сетки ролика [id], готовый или нет. */
    fun file(id: Long): File = File(dir, localStoryboardFileName(id))

    /**
     * Сетка ролика [id]: готовая с диска, иначе — результат идущей или новой
     * сборки. `null` — нет ролика, сборка не удалась или её отменили
     * ([cancel]); при следующем вызове сборка начнётся заново.
     */
    suspend fun obtain(id: Long): File? {
        if (id <= 0L) return null
        val ready = file(id)
        if (withContext(io) { ready.isFile && ready.length() > 0L }) return ready
        val build = synchronized(builds) { builds.getOrPut(id) { startBuild(id) } }
        return try {
            build.await()
        } catch (e: CancellationException) {
            // Отменили сборку (ролик удаляют) — сетки нет. Отмену самого
            // вызывающего пробрасываем.
            currentCoroutineContext().ensureActive()
            Timber.d("X сетка кадров: сборка отменена (%s)", e.javaClass.simpleName)
            null
        }
    }

    /** Собрать сетку ролика [id] в фоне — после скачивания. */
    fun prepare(id: Long) {
        scope.launch { obtain(id) }
    }

    /** Остановить идущую сборку ролика [id] — его удаляют. */
    fun cancel(id: Long) {
        synchronized(builds) { builds[id] }?.cancel()
    }

    private fun startBuild(id: Long): Deferred<File?> {
        val build = scope.async(io) { build(id) }
        build.invokeOnCompletion {
            synchronized(builds) { if (builds[id] === build) builds.remove(id) }
        }
        return build
    }

    private suspend fun build(id: Long): File? {
        val video = File(dir, "$id.mp4")
        if (!video.isFile) return null
        val temp = File(getTempPath(dir, localStoryboardFileName(id)))
        try {
            val rendered = try {
                renderer.render(video, temp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // В сообщении бывает путь к ролику — в журнал только класс.
                Timber.w("X сетка кадров: сборка не удалась: %s", e.javaClass.simpleName)
                false
            }
            currentCoroutineContext().ensureActive()
            // Ролик могли удалить, пока шла сборка: тогда сетка не нужна.
            if (!rendered || !video.isFile) return null
            val ready = file(id)
            return if (temp.renameTo(ready)) ready else null
        } finally {
            // После переименования временного файла уже нет; в остальных
            // случаях — недособранная сетка или отменённая сборка.
            temp.delete()
        }
    }
}
