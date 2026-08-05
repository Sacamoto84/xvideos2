package com.client.xvideos.l.featured.saved

import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.fileDB.folder.AppFileDatabase
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.l.model.AlbumDetails
import com.client.xvideos.l.model.PicsDetails
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class SavedL_Albums(val db: AppFileDatabase, val scope: CoroutineScope) {

    val albumDb = FileDB(AppPath.l_albums, "album", AlbumDetails::class.java)
    val list = albumDb.list

    fun add(item: AlbumDetails) {
        if (item.id.toLongOrNull() == null) {
            Timber.w("Skip saving L album with invalid id:${item.id} name:${item.title}")
            SnackBar.error("Альбом не сохранён: пустой id")
            return
        }

        Timber.i("addAlbum() id:${item.id} name:${item.title}")
        albumDb.insert(item.id, item)
            .onSuccess {
                SnackBar.info("Альбом сохранен")
                list.add(item)
            }
            .onFailure { e ->
                SnackBar.error("Ошибка добавления группы ${e.message}")
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
        albumDb.insert(item.id, item)
            .onSuccess {
                SnackBar.info("Альбом сохранен")
                list.add(item)

                scope.launch(Dispatchers.IO) {
                    val gson = Gson()
                    db.lAlbumPictureCache.put(albumId.toString(), gson.toJson(picsDetails))
                }

            }
            .onFailure { e ->
                SnackBar.error("Ошибка добавления группы ${e.message}")
            }
    }

    fun remove(item: AlbumDetails) {
        Timber.i("removeAlbum() id:${item.id} name:${item.title}")
        albumDb.delete(item.id)
            .onSuccess {
                SnackBar.info("Альбом удален")
                list.remove(item)
            }
            .onFailure { e ->
                SnackBar.error("Ошибка удаления группы ${e.message}")
            }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun refresh() {
        albumDb.refresh()
    }


}
