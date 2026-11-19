package com.client.xvideos.n.saved

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.n.model.NPicture
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий сохранённых лайков отдельных страниц/картинок манги nhentai на базе FileDB.
 */
@Singleton
class NLikesRepository @Inject constructor() {

    private val dbPath: String by lazy {
        File(AppPath.n_likes).apply { mkdirs() }.absolutePath
    }

    private val db: FileDB<NPicture> by lazy {
        FileDB(dbPath, "pic", NPicture.serializer()).apply {
            refresh()
        }
    }

    val likes: SnapshotStateList<NPicture>
        get() = db.list

    fun isLiked(id: String): Boolean {
        if (!AppPath.isInitialized) return false
        return likes.any { it.id == id } || db.contains(id)
    }

    fun isLiked(galleryId: Long, pageNumber: Int): Boolean =
        isLiked(NPicture.makeId(galleryId, pageNumber))

    @Synchronized
    fun toggleLike(picture: NPicture): Boolean {
        val liked = db.contains(picture.id)
        val result = if (liked) db.delete(picture.id) else db.insert(picture.id, picture)
        if (result.isFailure) {
            SnackBar.error("Не удалось изменить лайк")
            return liked
        }
        db.refresh()
        return !liked
    }

    fun addLike(picture: NPicture) {
        db.insert(picture.id, picture)
            .onSuccess { db.refresh() }
            .onFailure { SnackBar.error("Не удалось сохранить лайк") }
    }

    fun removeLike(id: String) {
        db.delete(id)
            .onSuccess { db.refresh() }
            .onFailure { SnackBar.error("Не удалось удалить лайк") }
    }

    fun removeLike(galleryId: Long, pageNumber: Int) {
        removeLike(NPicture.makeId(galleryId, pageNumber))
    }

    fun refresh() {
        if (AppPath.isInitialized) {
            db.refresh()
        }
    }
}
