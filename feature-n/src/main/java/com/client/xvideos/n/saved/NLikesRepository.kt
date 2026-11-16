package com.client.xvideos.n.saved

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
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

    fun toggleLike(picture: NPicture): Boolean {
        return if (db.contains(picture.id)) {
            db.delete(picture.id)
            db.refresh()
            false
        } else {
            db.insert(picture.id, picture)
            db.refresh()
            true
        }
    }

    fun addLike(picture: NPicture) {
        db.insert(picture.id, picture)
        db.refresh()
    }

    fun removeLike(id: String) {
        db.delete(id)
        db.refresh()
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
