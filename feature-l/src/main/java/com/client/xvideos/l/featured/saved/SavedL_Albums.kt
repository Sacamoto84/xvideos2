package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.json.AppJson
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Stable
import kotlinx.serialization.encodeToString
import timber.log.Timber

/**
 * Менеджер сохраненных альбомов Luscious в локальной файловой базе данных [FileDB].
 *
 * Предоставляет реактивный список [list], методы добавления, удаления и проверки наличия альбома.
 *
 * @property db Экземпляр файловой базы данных для доступа к сопутствующим кэшам.
 * @property scope Область корутин для выполнения асинхронных дисковых мутаций.
 */
@Stable
class SavedL_Albums(val db: AppFileDatabase, val scope: CoroutineScope) {

    /** Хранилище [FileDB] для альбомов Luscious по пути `AppPath.l_albums`. */
    val albumDb = FileDB(AppPath.l_albums, "album", AlbumDetails.serializer())
    /** Реактивный список сохраненных альбомов. */
    val list = albumDb.list
    private var mutationJob: Job? = null

    /**
     * Сохраняет метаданные альбома [item] в локальную базу данных.
     *
     * @param item Метаданные сохраняемого альбома.
     */
    fun add(item: AlbumDetails) {
        val albumId = item.id.toLongOrNull()
        if (albumId == null) {
            Timber.w("Skip saving L album with invalid id:${item.id} name:${item.title}")
            SnackBar.error("Альбом не сохранён: пустой id")
            return
        }

        Timber.i("addAlbum() id:${item.id} name:${item.title}")
        mutationJob?.cancel()
        mutationJob = scope.launch(Dispatchers.IO) {
            albumDb.insert(item.id, item)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.id == item.id }
                        list.add(item)
                    }
                    SnackBar.info("Альбом сохранен")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления альбома ${e.message}")
                }
        }
    }

    /**
     * Сохраняет альбом [item] вместе со списком его картинок [picsDetails] в дисковый кэш картинок.
     *
     * @param item Метаданные сохраняемого альбома.
     * @param picsDetails Список элементов изображений альбома.
     */
    fun addAndPicsDetails(item: AlbumDetails, picsDetails: List<PicsDetails>) {
        val albumId = item.id.toLongOrNull()
        if (albumId == null) {
            Timber.w("Skip saving L album with invalid id:${item.id} name:${item.title}")
            SnackBar.error("Альбом не сохранён: пустой id")
            return
        }

        Timber.i("addAndPicsDetails() id:${item.id} name:${item.title} picsDetails:${picsDetails.size}")
        mutationJob?.cancel()
        mutationJob = scope.launch(Dispatchers.IO) {
            albumDb.insert(item.id, item)
                .onSuccess {
                    if (picsDetails.isNotEmpty()) {
                        runCatching {
                            db.lAlbumPictureCache.put(albumId.toString(), AppJson.encodeToString(picsDetails))
                        }.onFailure {
                            Timber.e(it, "SavedL_Albums: ошибка кэширования картинок альбома $albumId")
                        }
                    }
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.id == item.id }
                        list.add(item)
                    }
                    SnackBar.info("Альбом сохранен")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления альбома ${e.message}")
                }
        }
    }

    /**
     * Удаляет альбом [item] из локального хранилища.
     *
     * @param item Удаляемый альбом.
     */
    fun remove(item: AlbumDetails) {
        if (item.id.isBlank()) return
        Timber.i("removeAlbum() id:${item.id} name:${item.title}")
        mutationJob?.cancel()
        mutationJob = scope.launch(Dispatchers.IO) {
            albumDb.delete(item.id)
                .onSuccess {
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.id == item.id }
                    }
                    SnackBar.info("Альбом удален")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка удаления альбома ${e.message}")
                }
        }
    }

    /**
     * Проверяет, сохранен ли альбом с идентификатором [id].
     *
     * @param id Строковый ID альбома.
     * @return `true`, если альбом присутствует в локальном списке.
     */
    fun contains(id: String): Boolean = id.isNotBlank() && list.any { it.id == id }

    private var refreshJob: Job? = null

    /**
     * Обновляет список сохраненных альбомов с диска.
     */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            albumDb.refresh()
        }
    }
}
