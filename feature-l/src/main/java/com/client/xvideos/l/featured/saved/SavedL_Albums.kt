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

@Stable
class SavedL_Albums(val db: AppFileDatabase, val scope: CoroutineScope) {

    val albumDb = FileDB(AppPath.l_albums, "album", AlbumDetails.serializer())
    val list = albumDb.list
    private var mutationJob: Job? = null

    fun add(item: AlbumDetails) {
        if (item.id.toLongOrNull() == null) {
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
                    SnackBar.error("Ошибка добавления группы ${e.message}")
                }
        }
    }

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
                    runCatching {
                        db.lAlbumPictureCache.put(albumId.toString(), AppJson.encodeToString(picsDetails))
                    }.onFailure {
                        Timber.e(it, "SavedL_Albums: ошибка кэширования картинок альбома $albumId")
                    }
                    withContext(Dispatchers.Main) {
                        list.removeAll { it.id == item.id }
                        list.add(item)
                    }
                    SnackBar.info("Альбом сохранен")
                }
                .onFailure { e ->
                    SnackBar.error("Ошибка добавления группы ${e.message}")
                }
        }
    }

    fun remove(item: AlbumDetails) {
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
                    SnackBar.error("Ошибка удаления группы ${e.message}")
                }
        }
    }

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch(Dispatchers.IO) {
            albumDb.refresh()
        }
    }
}
