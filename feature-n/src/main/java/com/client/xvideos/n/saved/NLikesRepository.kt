package com.client.xvideos.n.saved

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.common.snackbar.SnackBar
import com.client.xvideos.n.model.NPicture
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Репозиторий сохранённых лайков отдельных страниц/картинок манги nhentai на базе FileDB.
 */
@Singleton
class NLikesRepository @Inject constructor(@ApplicationScope scope: CoroutineScope) {

    private val db by lazy { FileDB(AppPath.n_likes, "pic", NPicture.serializer()) }
    private val mutex = Mutex()
    private var loaded = false

    init {
        scope.launch { load() }
    }

    val likes: SnapshotStateList<NPicture>
        get() = db.list

    fun isLiked(id: String): Boolean {
        if (!AppPath.isInitialized) return false
        return likes.any { it.id == id }
    }

    fun isLiked(galleryId: Long, pageNumber: Int): Boolean =
        isLiked(NPicture.makeId(galleryId, pageNumber))

    suspend fun toggleLike(picture: NPicture): Boolean = edit {
        val liked = db.contains(picture.id)
        val result = if (liked) db.delete(picture.id) else db.insert(picture.id, picture)
        if (result.isFailure) {
            SnackBar.error("Не удалось изменить лайк")
            return@edit liked
        }
        if (liked) likes.removeAll { it.id == picture.id }
        else publishLike(picture)
        !liked
    }

    suspend fun addLike(picture: NPicture) = edit {
        db.insert(picture.id, picture)
            .onSuccess { publishLike(picture) }
            .onFailure { SnackBar.error("Не удалось сохранить лайк") }
        Unit
    }

    suspend fun removeLike(id: String) = edit {
        db.delete(id)
            .onSuccess { likes.removeAll { it.id == id } }
            .onFailure { SnackBar.error("Не удалось удалить лайк") }
        Unit
    }

    suspend fun removeLike(galleryId: Long, pageNumber: Int) {
        removeLike(NPicture.makeId(galleryId, pageNumber))
    }

    internal suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (!loaded) refreshLocked()
        }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        mutex.withLock { refreshLocked() }
    }

    private fun refreshLocked() {
        if (AppPath.isInitialized && db.refresh().isSuccess) loaded = true
    }

    private suspend fun <T> edit(action: () -> T): T = withContext(NonCancellable) {
        mutex.withLock {
            withContext(Dispatchers.IO) {
                if (!loaded) refreshLocked()
                action()
            }
        }
    }

    private fun publishLike(picture: NPicture) = Snapshot.withMutableSnapshot {
        likes.removeAll { it.id == picture.id }
        likes.add(0, picture)
    }
}
