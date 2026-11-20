package com.client.xvideos.n.saved

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.di.ApplicationScope
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NGalleryPreview
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Репозиторий локально сохранённого (избранного) манги nhentai на базе FileDB.
 */
@Singleton
class NFavoritesRepository @Inject constructor(@ApplicationScope scope: CoroutineScope) {

    private val db: FileDB<NGalleryPreview> by lazy {
        FileDB(AppPath.n_favorites, "manga", NGalleryPreview.serializer())
    }

    var isLoaded by mutableStateOf(false)
        private set

    init {
        scope.launch { refresh() }
    }

    val favorites: SnapshotStateList<NGalleryPreview>
        get() = db.list

    fun isFavorite(id: Long): Boolean = AppPath.isInitialized && favorites.any { it.id == id }

    @Synchronized
    fun toggleFavorite(gallery: NGallery): Result<Boolean> {
        val idStr = gallery.id.toString()
        return if (db.readOrNull(idStr)?.id == gallery.id) {
            db.delete(idStr).map {
                db.refresh()
                false
            }
        } else {
            val preview = NGalleryPreview(
                id = gallery.id,
                mediaId = gallery.mediaId,
                title = gallery.title,
                thumbnailUrl = gallery.thumbnailUrl,
                numPages = gallery.numPages,
                language = gallery.languages.firstOrNull()?.name.orEmpty()
            )
            db.insert(idStr, preview).map {
                db.refresh()
                true
            }
        }
    }

    @Synchronized
    fun removeFavorite(id: Long): Result<Boolean> =
        db.delete(id.toString()).onSuccess { db.refresh() }

    /**
     * Импортирует одну галерею в локальное избранное.
     * Возвращает false для уже сохранённой галереи.
     */
    @Synchronized
    fun importFavorite(preview: NGalleryPreview): Result<Boolean> {
        val idStr = preview.id.toString()
        if (db.readOrNull(idStr)?.id == preview.id) return Result.success(false)
        return db.insert(idStr, preview).onSuccess { db.refresh() }
    }

    /**
     * Пакетно импортирует список галерей в локальное избранное.
     * Учитывает дубликаты и ошибки записи отдельно.
     */
    @Synchronized
    internal fun importFavorites(galleries: List<NGalleryPreview>): NFavoritesImport {
        var added = 0
        var skipped = 0
        var failed = 0
        galleries.forEach { preview ->
            val idStr = preview.id.toString()
            when {
                db.readOrNull(idStr)?.id == preview.id -> skipped++
                db.insert(idStr, preview).isSuccess -> added++
                else -> failed++
            }
        }
        if (added > 0) db.refresh()
        return NFavoritesImport(added, skipped, failed)
    }

    /**
     * Принудительно перечитывает закладки с диска (например, после восстановления бэкапа).
     */
    suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshFromDisk()
    }

    @Synchronized
    private fun refreshFromDisk() {
        if (AppPath.isInitialized) {
            db.refresh()
            isLoaded = true
        }
    }
}

internal data class NFavoritesImport(val added: Int, val skipped: Int, val failed: Int)
