package com.client.xvideos.n.saved

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.client.xvideos.common.AppPath
import com.client.xvideos.common.fileDB.FileDB
import com.client.xvideos.n.model.NGallery
import com.client.xvideos.n.model.NGalleryPreview
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Репозиторий локально сохранённого (избранного) манги nhentai на базе FileDB.
 */
@Singleton
class NFavoritesRepository @Inject constructor() {

    private val dbPath: String by lazy {
        File(AppPath.n_favorites).apply { mkdirs() }.absolutePath
    }

    private val db: FileDB<NGalleryPreview> by lazy {
        FileDB(dbPath, "manga", NGalleryPreview.serializer()).apply {
            refresh()
        }
    }

    val favorites: SnapshotStateList<NGalleryPreview>
        get() = db.list

    fun isFavorite(id: Long): Boolean {
        if (!AppPath.isInitialized) return false
        return favorites.any { it.id == id } || db.contains(id.toString())
    }

    @Synchronized
    fun toggleFavorite(gallery: NGallery): Result<Boolean> {
        val idStr = gallery.id.toString()
        return if (db.contains(idStr)) {
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
        if (db.contains(idStr)) return Result.success(false)
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
                db.contains(idStr) -> skipped++
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
    fun refresh() {
        if (AppPath.isInitialized) {
            db.refresh()
        }
    }
}

internal data class NFavoritesImport(val added: Int, val skipped: Int, val failed: Int)
