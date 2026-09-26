package com.client.xvideos.r.common.saved

import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.common.util.replaceWith
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.model.sanitizeGifsInfoList
import com.client.xvideos.r.model.sanitizeOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Хранилище понравившихся роликов (лайков) RedGifs на базе [FileDB].
 *
 * Сохраняет JSON-файлы с метаданными [GifsInfo] в директорию `AppPath.r_likes` с расширением `.likes`.
 * Предоставляет реактивный Compose-список [list] для непосредственного отображения в UI.
 *
 * @param scope Корутин-скоп для асинхронных операций чтения и записи.
 */
class R_Saved_Likes(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    /** Файловая БД лайков. */
    val likesDb = FileDB(AppPath.r_likes, "likes", GifsInfo.serializer())

    /** Реактивный список лайков для Compose UI. */
    val list = likesDb.list

    /**
     * Добавляет [item] в лайки: сохраняет файл на диск и обновляет список в памяти.
     */
    fun add(item: GifsInfo) {
        val safeItem = item.sanitizeOrNull() ?: run {
            SnackBar.error("Like add error: empty id")
            return
        }
        Timber.i(
            "R_Saved_Likes add() id:${safeItem.id} userName:${safeItem.userName} " +
                "url:${safeItem.urls.hd} -> ${likesDb.dirPath}/${safeItem.id}.likes"
        )
        scope.launch(Dispatchers.IO) {
            likesDb.insert(safeItem.id, safeItem)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = list.indexOfFirst { it.id == safeItem.id }
                        if (existingIndex == list.lastIndex && list[existingIndex] == safeItem) {
                            return@withContext
                        }
                        if (existingIndex >= 0) {
                            list.removeAt(existingIndex)
                        }
                        list.add(safeItem)
                    }
                    SnackBar.success("Like")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления лайка ${e.message}")
                }
        }
    }

    /**
     * Удаляет [item] из лайков на диске и в памяти.
     */
    fun remove(item: GifsInfo) {
        if (item.id.isBlank()) return
        Timber.i("R_Saved_Likes remove() id:${item.id} userName:${item.userName} url:${item.urls.hd}")
        scope.launch(Dispatchers.IO) {
            likesDb.delete(item.id)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        val existingIndex = list.indexOfFirst { it.id == item.id }
                        if (existingIndex >= 0) {
                            list.removeAt(existingIndex)
                        }
                    }
                    SnackBar.info("Unlike")
                }
                .onFailure { e -> SnackBar.error("Ошибка удаления лайка ${e.message}") }
        }
    }

    /** Быстрая проверка принадлежности к лайкам по строковому [id]. */
    fun contains(id: String): Boolean = id.isNotBlank() && list.any { it.id == id }

    private var refreshJob: Job? = null

    /**
     * Перечитывает все сохраненные лайки с диска с автоматической очисткой поврежденных записей.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            likesDb.refresh()
            val current = withContext(Dispatchers.Main) { list.toList() }
            if (current.isEmpty()) return@launch
            val sanitized = current.sanitizeGifsInfoList()
            // Переписываем список только если санитизация реально что-то изменила,
            // иначе получаем лишнюю перезапись и мигание списка.
            if (sanitized != current) {
                withContext(Dispatchers.Main) {
                    list.replaceWith(sanitized)
                }
            }
        }
    }
}
